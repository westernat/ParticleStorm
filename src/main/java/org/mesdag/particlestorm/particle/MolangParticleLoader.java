package org.mesdag.particlestorm.particle;

import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.data.component.IComponent;
import org.mesdag.particlestorm.data.component.IParticleComponent;
import org.mesdag.particlestorm.network.EmitterRemovalPacket;
import org.mesdag.particlestorm.network.EmitterSynchronizePacket;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@OnlyIn(Dist.CLIENT)
public class MolangParticleLoader implements PreparableReloadListener {
    private static final FileToIdConverter PARTICLE_LISTER = FileToIdConverter.json("particle_definitions");
    public final Hashtable<ResourceLocation, DefinedParticleEffect> ID_2_EFFECT = new Hashtable<>();
    public final Hashtable<ResourceLocation, ParticleDetail> ID_2_PARTICLE = new Hashtable<>();
    public final Hashtable<ResourceLocation, EmitterDetail> ID_2_EMITTER = new Hashtable<>();
    public final Int2ObjectOpenHashMap<ParticleEmitter> emitters = new Int2ObjectOpenHashMap<>();
    private final Queue<Integer> emittersAboutToRemove = new ArrayDeque<>();
    private final IntAllocator allocator = new IntAllocator();

    private Player player;
    private int renderDistSqr = 1024;
    private boolean initialized = false;

    public void tick() {
        if (!initialized) {
            this.player = Minecraft.getInstance().player;
            if (player == null) return;
            for (ParticleDetail detail : ID_2_PARTICLE.values()) {
                for (IComponent component : detail.effect.components.values()) {
                    if (component instanceof IParticleComponent particleComponent) {
                        particleComponent.initialize(player.level());
                    }
                }
            }
            Integer i = Minecraft.getInstance().options.renderDistance().get() * 16;
            this.renderDistSqr = i * i;
            this.initialized = true;
        } else if (emittersAboutToRemove.isEmpty()) {
            ObjectIterator<Int2ObjectMap.Entry<ParticleEmitter>> iterator = emitters.int2ObjectEntrySet().iterator();
            while (iterator.hasNext()) {
                ParticleEmitter emitter = iterator.next().getValue();
                if (emitter.isRemoved()) {
                    allocator.remove(emitter.id);
                    iterator.remove();
                } else if (emitter.pos.distanceToSqr(player.position()) < renderDistSqr) {
                    emitter.tick();
                }
            }
        } else {
            while (!emittersAboutToRemove.isEmpty()) {
                int removed = emittersAboutToRemove.remove();
                emitters.remove(removed);
                allocator.remove(removed);
            }
        }
    }

    public int totalEmitterCount() {
        return emitters.size();
    }

    public ParticleEmitter getEmitter(int id) {
        return emitters.get(id);
    }

    public void loadEmitter(Player player, int id, CompoundTag tag) {
        ParticleEmitter emitter = new ParticleEmitter(player.level(), tag);
        emitter.id = id;
        emitters.put(id, emitter);
        if (allocator.forceAdd(id)) {
            ParticleStorm.LOGGER.warn("There was an emitter exist before, now replaced");
        }
    }

    public void addEmitter(ParticleEmitter emitter, boolean sync) {
        emitter.id = allocator.insert();
        emitters.put(emitter.id, emitter);
        if (sync) EmitterSynchronizePacket.syncToServer(emitter);
    }

    public void removeEmitter(int id, boolean sync) {
        emittersAboutToRemove.add(id);
        if (sync) EmitterRemovalPacket.sendToServer(id);
    }

    public void removeAll() {
        emitters.clear();
        emittersAboutToRemove.clear();
        allocator.clear();
        this.initialized = false;
    }

    public boolean contains(int id) {
        return allocator.table.contains(id);
    }

    @Override
    public @NotNull CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller preparationsProfiler, @NotNull ProfilerFiller reloadProfiler, @NotNull Executor backgroundExecutor, @NotNull Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> PARTICLE_LISTER.listMatchingResources(resourceManager), backgroundExecutor).thenCompose(map -> {
            List<CompletableFuture<DefinedParticleEffect>> list = new ArrayList<>(map.size());
            map.forEach((file, resource) -> {
                ResourceLocation id = PARTICLE_LISTER.fileToId(file);
                list.add(CompletableFuture.supplyAsync(() -> {
                    try (Reader reader = resource.openAsReader()) {
                        return DefinedParticleEffect.CODEC.parse(JsonOps.INSTANCE, GsonHelper.parse(reader).get("particle_effect")).getOrThrow(JsonParseException::new);
                    } catch (IOException exception) {
                        throw new IllegalStateException("Failed to load definition for particle " + id, exception);
                    }
                }, backgroundExecutor));
            });
            return Util.sequence(list);
        }).thenCompose(preparationBarrier::wait).thenAcceptAsync(effects -> {
            ID_2_EFFECT.clear();
            ID_2_PARTICLE.clear();
            ID_2_EMITTER.clear();
            effects.forEach(effect -> {
                ResourceLocation id = effect.description.identifier();
                ID_2_EFFECT.put(id, effect);
                ID_2_PARTICLE.put(id, new ParticleDetail(effect));
                ID_2_EMITTER.put(id, new EmitterDetail(
                        new MolangParticleOption(effect.description.identifier()),
                        effect.orderedEmitterComponents,
                        effect.events
                ));
            });
        }, gameExecutor);
    }

    private static class IntAllocator {
        private final IntArraySet table;

        public IntAllocator() {
            this.table = new IntArraySet();
        }

        public int insert() {
            int size = table.size();
            for (int i = 0; i < size; i++) {
                if (!table.contains(i)) {
                    table.add(i);
                    return i;
                }
            }
            table.add(size);
            return size;
        }

        public boolean forceAdd(int id) {
            return table.add(id);
        }

        public void remove(int id) {
            table.remove(id);
        }

        public void clear() {
            table.clear();
        }
    }
}

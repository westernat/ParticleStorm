package org.mesdag.particlestorm.particle;

import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.Util;
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
import org.mesdag.particlestorm.data.component.IEmitterComponent;
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
    private final ArrayList<ParticleEmitter> emitters = new ArrayList<>();
    private final Queue<Integer> emittersAboutToRemove = new ArrayDeque<>();
    private final IntAllocator allocator = new IntAllocator();

    public void tick() {
        if (emittersAboutToRemove.isEmpty()) {
            Iterator<ParticleEmitter> iterator = emitters.iterator();
            while (iterator.hasNext()) {
                ParticleEmitter emitter = iterator.next();
                if (emitter.isRemoved()) {
                    allocator.remove(emitter.id);
                    iterator.remove();
                } else { // todo distance
                    emitter.tick();
                }
            }
        } else {
            while (!emittersAboutToRemove.isEmpty()) {
                int removed = emittersAboutToRemove.remove();
                emitters.removeIf(emitter -> emitter.id == removed);
                allocator.remove(removed);
            }
        }
    }

    public void loadEmitter(Player player, int id, CompoundTag tag) {
        ParticleEmitter emitter = new ParticleEmitter(player.level(), tag);
        emitters.add(emitter);
        emitter.id = id;
        if (allocator.forceAdd(id)) {
            ParticleStorm.LOGGER.warn("There was an emitter exist before, now replaced");
        }
    }

    public void addEmitter(ParticleEmitter emitter, boolean sync) {
        emitters.add(emitter);
        emitter.id = allocator.insert();
        if (sync) EmitterSynchronizePacket.syncToServer(emitter);
    }

    public void removeEmitter(int id, boolean sync) {
        emittersAboutToRemove.add(id);
        if (sync) EmitterRemovalPacket.sendToServer(id);
    }

    public void removeEmitter(ParticleEmitter emitter, boolean sync) {
        removeEmitter(emitter.id, sync);
    }

    public void removeAll() {
        emitters.clear();
        emittersAboutToRemove.clear();
        allocator.clear();
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
                        effect.components.values().stream()
                                .filter(c -> c instanceof IEmitterComponent)
                                .map(c -> (IEmitterComponent) c).toList(),
                        effect.events
                ));
            });
        }, gameExecutor);
    }

    private static class IntAllocator {
        private final IntOpenHashSet table;

        public IntAllocator() {
            this.table = new IntOpenHashSet();
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

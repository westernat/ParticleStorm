package org.mesdag.particlestorm.particle;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Lists;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModLoader;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.*;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.network.EmitterRemovalPacket;
import org.mesdag.particlestorm.network.EmitterSynchronizePacket;

import java.io.IOException;
import java.io.Reader;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@SuppressWarnings("all")
public class MolangParticleLoader implements PreparableReloadListener {
    private static final FileToIdConverter PARTICLE_LISTER = FileToIdConverter.json("particle_definitions");
    private Map<ResourceLocation, DefinedParticleEffect> id2Effect = new Hashtable<>();
    private Map<ResourceLocation, ParticlePreset> id2Particle = new Hashtable<>();
    private Map<ResourceLocation, EmitterPreset> id2Emitter = new Hashtable<>();
    private final Int2ObjectOpenHashMap<ParticleEmitter> emitters = new Int2ObjectOpenHashMap<>();
    private final Object2ObjectMap<Entity, EvictingQueue<ParticleEmitter>> tracker = new Object2ObjectOpenHashMap<>();
    private final IntAllocator allocator = new IntAllocator();

    private boolean initialized = false;

    public Map<ResourceLocation, DefinedParticleEffect> id2Effect() {
        return id2Effect;
    }

    public Map<ResourceLocation, ParticlePreset> id2Particle() {
        return id2Particle;
    }

    public Map<ResourceLocation, EmitterPreset> id2Emitter() {
        return id2Emitter;
    }

    public void tick(LocalPlayer localPlayer) {
        if (!initialized) {
            for (ParticlePreset detail : id2Particle.values()) {
                for (IComponent component : detail.effect.orderedComponents) {
                    if (component instanceof IParticleComponent particleComponent) {
                        particleComponent.initialize(localPlayer.level());
                    }
                }
            }
            removeAll();
            this.initialized = true;
        }
        if (!emitters.isEmpty()) {
            int renderDistSqr = Mth.square(Minecraft.getInstance().options.renderDistance().get() * 16);
            ObjectIterator<Int2ObjectMap.Entry<ParticleEmitter>> iterator = emitters.int2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                ParticleEmitter emitter = iterator.next().getValue();
                try {
                    if (emitter.isRemoved() || emitter.level.dimension() != localPlayer.level().dimension()) {
                        emitter.onRemove();
                        emitter.remove();
                        iterator.remove();
                        allocator.release(emitter.id);
                    } else if (Mth.lengthSquared(emitter.pos.x - localPlayer.getX(), emitter.pos.z - localPlayer.getZ()) < renderDistSqr) {
                        emitter.tick();
                    }
                } catch (Exception e) {
                    ParticleStorm.LOGGER.warn("Error ticking: {}", e.getMessage());
                    e.printStackTrace();
                    if (emitter != null) {
                        emitter.remove();
                    }
                    iterator.remove();
                }
            }
        }
        if (!tracker.isEmpty()) {
            ObjectIterator<Map.Entry<Entity, EvictingQueue<ParticleEmitter>>> iterator1 = tracker.entrySet().iterator();
            while (iterator1.hasNext()) {
                Map.Entry<Entity, EvictingQueue<ParticleEmitter>> entry = iterator1.next();
                if (entry.getKey().isRemoved()) {
                    iterator1.remove();
                } else if (entry.getValue().removeIf(ParticleEmitter::isRemoved) && entry.getValue().isEmpty()) {
                    iterator1.remove();
                }
            }
        }
    }

    public Iterable<ParticleEmitter> getEmitters() {
        return emitters.values();
    }

    public int totalEmitterCount() {
        return emitters.size();
    }

    public void loadEmitter(Level level, int id, CompoundTag tag) {
        ParticleEmitter emitter = RegisterCustomEmitterTypeEvent.create(level, tag);
        emitter.id = id;
        emitters.put(id, emitter);
        if (allocator.forceAllocate(id)) {
            ParticleStorm.LOGGER.warn("There was an emitter exist before, now replaced");
        }
    }

    public void addEmitter(ParticleEmitter emitter, boolean sync) {
        emitter.id = allocator.allocate();
        emitters.put(emitter.id, emitter);
        if (sync) EmitterSynchronizePacket.syncToServer(emitter);
    }

    public boolean addTrackedEmitter(Entity entity, ResourceLocation particleId) {
        EvictingQueue<ParticleEmitter> queue = tracker.computeIfAbsent(entity, e -> EvictingQueue.create(16));
        if (!queue.isEmpty() && queue.stream().anyMatch(emitter -> particleId.equals(emitter.particleId))) return false;
        ParticleEmitter emitter = new ParticleEmitter(entity.level(), entity.position(), particleId);
        addEmitter(emitter, false);
        emitter.attachEntity(entity);
        queue.add(emitter);
        return true;
    }

    public void removeEmitter(ParticleEmitter emitter, boolean sync) {
        removeEmitter(emitter.id, sync);
    }

    public @Nullable ParticleEmitter removeEmitter(int id, boolean sync) {
        ParticleEmitter removed = emitters.remove(id);
        if (removed != null) {
            removed.onRemove();
        }
        allocator.release(id);
        if (sync) EmitterRemovalPacket.sendToServer(id);
        return removed;
    }

    public void removeAll() {
        if (!emitters.isEmpty()) {
            ObjectIterator<Int2ObjectMap.Entry<ParticleEmitter>> iterator = emitters.int2ObjectEntrySet().iterator();
            while (iterator.hasNext()) {
                iterator.next().getValue().remove();
                iterator.remove();
            }
        }
        tracker.clear();
        allocator.clear();
    }

    public boolean contains(int id) {
        return allocator.isAllocated(id);
    }

    public @Nullable ParticleEmitter getEmitter(int id) {
        return emitters.get(id);
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> PARTICLE_LISTER.listMatchingResources(resourceManager), backgroundExecutor).thenCompose(map -> {
            ModLoader.postEvent(new MolangParticleLoadEvent.Pre(backgroundExecutor));
            List<CompletableFuture<DefinedParticleEffect>> list = Lists.newArrayListWithExpectedSize(map.size());
            for (Map.Entry<ResourceLocation, Resource> entry : map.entrySet()) {
                ResourceLocation id = PARTICLE_LISTER.fileToId(entry.getKey());
                list.add(CompletableFuture.supplyAsync(() -> {
                    try (Reader reader = entry.getValue().openAsReader()) {
                        return DefinedParticleEffect.CODEC.parse(JsonOps.INSTANCE, GsonHelper.parse(reader).get("particle_effect")).getOrThrow(JsonParseException::new);
                    } catch (IOException exception) {
                        throw new IllegalStateException("Failed to load definition for particle " + id, exception);
                    }
                }, backgroundExecutor));
            }
            return Util.sequence(list);
        }).thenCompose(preparationBarrier::wait).thenAcceptAsync(effects -> {
            Map<ResourceLocation, DefinedParticleEffect> id2Effect = new Hashtable<>();
            Map<ResourceLocation, ParticlePreset> id2Particle = new Hashtable<>();
            Map<ResourceLocation, EmitterPreset> id2Emitter = new Hashtable<>();
            for (DefinedParticleEffect effect : effects) {
                ResourceLocation id = effect.description.identifier();
                id2Effect.put(id, effect);
                id2Particle.put(id, new ParticlePreset(effect));
                id2Emitter.put(id, new EmitterPreset(
                        effect.description.type(),
                        effect.orderedEmitterComponents,
                        effect.events
                ));
            }
            this.id2Effect = id2Effect;
            this.id2Particle = id2Particle;
            this.id2Emitter = id2Emitter;
            this.initialized = false;
            ModLoader.postEvent(new MolangParticleLoadEvent.Post(gameExecutor));
        }, gameExecutor);
    }
}

package org.mesdag.particlestorm.particle;

import com.google.common.collect.Lists;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.PSClientConfigs;
import org.mesdag.particlestorm.PSDiagnostics;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.api.IntAllocator;
import org.mesdag.particlestorm.api.RegisterCustomEmitterTypeEvent;
import org.mesdag.particlestorm.api.RegisterCustomParticleTypeEvent;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.network.EmitterRemovalPacket;
import org.mesdag.particlestorm.network.EmitterSynchronizePacket;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@SuppressWarnings("all")
public final class MolangParticleEngine implements PreparableReloadListener {
    public static final MolangParticleEngine INSTANCE = new MolangParticleEngine();
    public static final Identifier RELOADER_ID = ParticleStorm.asResource("reloader");
    private static final FileToIdConverter PARTICLE_LISTER = FileToIdConverter.json("particle_definitions");
    private Map<Identifier, DefinedParticleEffect> id2Effect = new Hashtable<>();
    private Map<Identifier, ParticlePreset> id2Particle = new Hashtable<>();
    private Map<Identifier, EmitterPreset> id2Emitter = new Hashtable<>();
    private final Int2ObjectOpenHashMap<ParticleEmitter> emitters = new Int2ObjectOpenHashMap<>();
    private final Object2ObjectOpenCustomHashMap<Entity, Object2ObjectLinkedOpenHashMap<Identifier, ParticleEmitter>> tracker = new Object2ObjectOpenCustomHashMap<>(new Hash.Strategy<>() {
        @Override
        public int hashCode(Entity o) {
            return o.getUUID().hashCode();
        }

        @Override
        public boolean equals(Entity a, Entity b) {
            return a.getUUID().equals(b.getUUID());
        }
    });
    private final Int2ObjectOpenHashMap<Queue<IMolangParticleInstance>> particlesForEmitter = new Int2ObjectOpenHashMap<>();
    private final IntAllocator allocator = new IntAllocator();

    private boolean initialized = false;

    private MolangParticleEngine() {
    }

    public Map<Identifier, DefinedParticleEffect> id2Effect() {
        return id2Effect;
    }

    public Map<Identifier, ParticlePreset> id2Particle() {
        return id2Particle;
    }

    public Map<Identifier, EmitterPreset> id2Emitter() {
        return id2Emitter;
    }

    public @Nullable Identifier resolveParticleId(Identifier id) {
        if (id2Emitter.containsKey(id)) {
            return id;
        }
        Identifier normalized = stripParticleSuffix(id);
        return id2Emitter.containsKey(normalized) ? normalized : null;
    }

    public boolean containsParticle(Identifier id) {
        return resolveParticleId(id) != null;
    }

    public Set<Identifier> suggestibleParticleIds() {
        return new HashSet<>(id2Emitter.keySet());
    }

    public void tick(LocalPlayer localPlayer) {
        if (!initialized) {
            for (ParticlePreset detail : new HashSet<>(id2Particle.values())) {
                for (IParticleComponent component : detail.effect.orderedParticleComponents) {
                    component.initialize(localPlayer.level());
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
                        removeEmitterNoUpdate(emitter);
                        iterator.remove();
                    } else if (Mth.square(emitter.pos.x - localPlayer.getX()) + Mth.square(emitter.pos.z - localPlayer.getZ()) < renderDistSqr) {
                        emitter.tick();
                    }
                } catch (Exception e) {
                    ParticleStorm.LOGGER.warn("Error ticking: {}", e.getMessage());
                    e.printStackTrace();
                    if (emitter != null) {
                        removeEmitterNoUpdate(emitter);
                    }
                    iterator.remove();
                }
            }
        }
        if (!tracker.isEmpty()) {
            var iterator1 = tracker.object2ObjectEntrySet().fastIterator();
            while (iterator1.hasNext()) {
                var entry = iterator1.next();
                if (entry.getKey().isRemoved()) {
                    iterator1.remove();
                } else if (entry.getValue().values().removeIf(ParticleEmitter::isRemoved) && entry.getValue().isEmpty()) {
                    iterator1.remove();
                }
            }
        }
        if (!particlesForEmitter.isEmpty()) {
            var iterator2 = particlesForEmitter.int2ObjectEntrySet().fastIterator();
            while (iterator2.hasNext()) {
                iterator2.next().getValue().removeIf(IMolangParticleInstance::isDiscarded);
            }
        }
    }

    public Iterable<ParticleEmitter> getEmitters() {
        return emitters.values();
    }

    public int totalEmitterCount() {
        return emitters.size();
    }

    public int totalParticleCount() {
        return particlesForEmitter.values().stream().mapToInt(Queue::size).sum();
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

    /// Client-side emitter (block attach or preset vars) that must not be persisted to the server.
    public void addEmitter(ParticleEmitter emitter) {
        addEmitter(emitter, false);
    }

    public boolean addTrackedEmitter(Entity entity, Identifier particleId) {
        Object2ObjectLinkedOpenHashMap<Identifier, ParticleEmitter> queue = tracker.computeIfAbsent(entity, e -> new Object2ObjectLinkedOpenHashMap<>());
        if (!queue.isEmpty() && queue.containsKey(particleId)) return false;
        ParticleEmitter emitter = new ParticleEmitter(entity.level(), entity.position(), particleId);
        addEmitter(emitter, false);
        emitter.attachEntity(entity);
        queue.put(particleId, emitter);
        if (queue.size() > PSClientConfigs.maxTrackersPerEntity) {
            queue.removeFirst();
        }
        return true;
    }

    public @Nullable Queue<IMolangParticleInstance> getParticlesForEmitter(ParticleEmitter emitter) {
        return particlesForEmitter.get(emitter.id);
    }

    public void registerParticle(ParticleEmitter emitter, IMolangParticleInstance instance) {
        particlesForEmitter.computeIfAbsent(emitter.id, id -> new ArrayDeque<>()).add(instance);
    }

    public void unregisterParticle(IMolangParticleInstance instance) {
        ParticleEmitter emitter = instance.getEmitter();
        if (emitter == null) return;
        Queue<IMolangParticleInstance> queue = particlesForEmitter.get(emitter.id);
        if (queue != null) {
            queue.remove(instance);
        }
    }

    public void removeEmitter(ParticleEmitter emitter, boolean sync) {
        removeEmitter(emitter.id, sync);
    }

    private void removeEmitterNoUpdate(ParticleEmitter emitter) {
        emitter.onRemove();
        emitter.remove();
        allocator.release(emitter.id);
        particlesForEmitter.remove(emitter.id);
    }

    public ParticleEmitter removeEmitter(int id, boolean sync) {
        ParticleEmitter removed = emitters.remove(id);
        if (removed == null) {
            return null;
        }
        removeEmitterNoUpdate(removed);
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
        particlesForEmitter.clear();
        allocator.clear();
    }

    public boolean contains(int id) {
        return allocator.isAllocated(id);
    }

    public @Nullable ParticleEmitter getEmitter(int id) {
        return emitters.get(id);
    }

    @Override
    public CompletableFuture<Void> reload(SharedState sharedState, Executor backgroundExecutor, PreparationBarrier preparationBarrier, Executor gameExecutor) {
        ResourceManager resourceManager = sharedState.resourceManager();
        return CompletableFuture.supplyAsync(() -> PARTICLE_LISTER.listMatchingResources(resourceManager), backgroundExecutor).thenCompose(map -> {
            List<CompletableFuture<LoadedParticle>> list = Lists.newArrayListWithExpectedSize(map.size());
            for (Map.Entry<Identifier, Resource> entry : map.entrySet()) {
                Identifier id = PARTICLE_LISTER.fileToId(entry.getKey());
                list.add(CompletableFuture.supplyAsync(() -> {
                    try (Reader reader = entry.getValue().openAsReader()) {
                        DefinedParticleEffect effect = DefinedParticleEffect.CODEC.parse(JsonOps.INSTANCE, GsonHelper.parse(reader).get("particle_effect")).getOrThrow(JsonParseException::new);
                        return new LoadedParticle(id, effect);
                    } catch (Exception exception) {
                        PSDiagnostics.error("failed to load particle definition fileId={} resource={}", id, entry.getKey(), exception);
                        throw new IllegalStateException("Failed to load definition for particle " + id, exception);
                    }
                }, backgroundExecutor));
            }
            return Util.sequence(list);
        }).thenCompose(preparationBarrier::wait).thenAcceptAsync(effects -> {
            PSDiagnostics.clear();
            Map<Identifier, DefinedParticleEffect> id2Effect = new Hashtable<>();
            Map<Identifier, ParticlePreset> id2Particle = new Hashtable<>();
            Map<Identifier, EmitterPreset> id2Emitter = new Hashtable<>();
            for (LoadedParticle loaded : effects) {
                DefinedParticleEffect effect = loaded.effect();
                Identifier id = effect.description.identifier();
                Set<Identifier> aliases = new LinkedHashSet<>();
                aliases.add(id);
                aliases.add(loaded.fileId());
                aliases.add(stripParticleSuffix(loaded.fileId()));
                ParticlePreset particlePreset = new ParticlePreset(effect);
                EmitterPreset emitterPreset = new EmitterPreset(
                        effect.description.type(),
                        effect.orderedEmitterComponents,
                        effect.events
                );
                for (Identifier alias : aliases) {
                    registerAlias(id2Effect, id2Particle, id2Emitter, alias, effect, particlePreset, emitterPreset);
                }
                PSDiagnostics.info("loaded definition fileId={} identifier={} aliases={} material={} texture={} emitterComponents={} particleComponents={} events={}",
                        loaded.fileId(),
                        id,
                        aliases,
                        effect.description.parameters().material(),
                        effect.description.parameters().texture(),
                        effect.orderedEmitterComponents.stream().map(component -> component.getClass().getSimpleName()).toList(),
                        effect.orderedParticleComponents.stream().map(component -> component.getClass().getSimpleName()).toList(),
                        effect.events.keySet()
                );
            }
            this.id2Effect = id2Effect;
            this.id2Particle = id2Particle;
            this.id2Emitter = id2Emitter;
            RegisterCustomParticleTypeEvent.bindSprites(id2Effect);
            this.initialized = false;
            ParticleStorm.LOGGER.info("Loaded {} particle definitions with {} usable ids", effects.size(), id2Emitter.size());
        }, gameExecutor);
    }

    private static void registerAlias(
            Map<Identifier, DefinedParticleEffect> id2Effect,
            Map<Identifier, ParticlePreset> id2Particle,
            Map<Identifier, EmitterPreset> id2Emitter,
            Identifier id,
            DefinedParticleEffect effect,
            ParticlePreset particlePreset,
            EmitterPreset emitterPreset
    ) {
        DefinedParticleEffect previous = id2Effect.get(id);
        if (previous != null && previous != effect) {
            ParticleStorm.LOGGER.warn("Duplicate ParticleStorm particle id '{}'; keeping the first loaded definition", id);
            return;
        }
        id2Effect.put(id, effect);
        id2Particle.put(id, particlePreset);
        id2Emitter.put(id, emitterPreset);
    }

    private static Identifier stripParticleSuffix(Identifier id) {
        String path = id.getPath();
        if (path.endsWith(".particle")) {
            return Identifier.fromNamespaceAndPath(id.getNamespace(), path.substring(0, path.length() - ".particle".length()));
        }
        return id;
    }

    private record LoadedParticle(Identifier fileId, DefinedParticleEffect effect) {
    }
}

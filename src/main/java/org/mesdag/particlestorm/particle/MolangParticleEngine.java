package org.mesdag.particlestorm.particle;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModLoader;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.PSClientConfigs;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.*;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.particle.screen.IParticleManagerScreen;
import org.mesdag.particlestorm.particle.screen.ScreenParticleEmitter;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class MolangParticleEngine extends MolangParticleManager implements PreparableReloadListener {
    public static final MolangParticleEngine INSTANCE = new MolangParticleEngine();
    private static final FileToIdConverter PARTICLE_LISTER = FileToIdConverter.json("particle_definitions");
    private static MolangParticleManager screenManager;

    private Map<ResourceLocation, DefinedParticleEffect> id2Effect = ImmutableMap.of();
    private Map<ResourceLocation, ParticlePreset> id2Particle = ImmutableMap.of();
    private Map<ResourceLocation, EmitterPreset> id2Emitter = ImmutableMap.of();

    private final Object2ObjectOpenCustomHashMap<Entity, Object2ObjectLinkedOpenHashMap<ResourceLocation, ParticleEmitter>> tracker = new Object2ObjectOpenCustomHashMap<>(new Hash.Strategy<>() {
        @Override
        public int hashCode(Entity o) {
            return o.getUUID().hashCode();
        }

        @Override
        public boolean equals(@Nullable Entity a, @Nullable Entity b) {
            return a == b || (a != null && b != null && a.getUUID().equals(b.getUUID()));
        }
    });
    private boolean initialized = false;

    private MolangParticleEngine() {
        super(new IntAllocator());
    }

    public Map<ResourceLocation, DefinedParticleEffect> id2Effect() {
        return id2Effect;
    }

    public Map<ResourceLocation, ParticlePreset> id2Particle() {
        return id2Particle;
    }

    public Map<ResourceLocation, EmitterPreset> id2Emitter() {
        return id2Emitter;
    }

    @Override
    public void tick(Minecraft minecraft, LocalPlayer player) {
        if (!initialized) {
            for (ParticlePreset detail : id2Particle.values()) {
                for (IComponent component : detail.effect.orderedComponents) {
                    if (component instanceof IParticleComponent particleComponent) {
                        particleComponent.initialize(player.clientLevel);
                    }
                }
            }
            if (ParticleStorm.GECKOLIB_LOADED) {
                GeckoLibHelper.afterReload();
            }
            removeAll();
            this.initialized = true;
        }
        screenManager = minecraft.screen instanceof IParticleManagerScreen screen ? screen.getManager() : null;
        super.tick(minecraft, player);
        if (!tracker.isEmpty()) {
            var iterator = tracker.object2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (entry.getKey().isRemoved()) {
                    iterator.remove();
                } else if (entry.getValue().values().removeIf(ParticleEmitter::isRemoved) && entry.getValue().isEmpty()) {
                    iterator.remove();
                }
            }
        }
    }

    public static void addParticle(IMolangParticleInstance instance) {
        if (screenManager != null && instance.getEmitter() instanceof ScreenParticleEmitter) {
            screenManager._addParticle(instance);
        } else {
            INSTANCE._addParticle(instance);
        }
    }

    public static void addEmitter(ParticleEmitter emitter) {
        if (screenManager != null && emitter instanceof ScreenParticleEmitter) {
            screenManager._addEmitter(emitter);
        } else {
            INSTANCE._addEmitter(emitter);
        }
    }

    public static void removeEmitter(int id) {
        if (screenManager != null && screenManager._removeEmitter(id) instanceof ScreenParticleEmitter) {
            return;
        }
        INSTANCE._removeEmitter(id);
    }

    public boolean addTrackedEmitter(Entity entity, ResourceLocation particleId, boolean hideOutline) {
        var queue = tracker.computeIfAbsent(entity, e -> new Object2ObjectLinkedOpenHashMap<>());
        if (!queue.isEmpty() && queue.containsKey(particleId)) return false;
        ParticleEmitter emitter = new ParticleEmitter(entity.level(), entity.position(), particleId);
        emitter.hideOutline = hideOutline;
        _addEmitter(emitter);
        emitter.attachEntity(entity);
        queue.put(particleId, emitter);
        if (queue.size() > PSClientConfigs.maxTrackersPerEntity) {
            queue.removeFirst();
        }
        return true;
    }

    public boolean addTrackedEmitter(Entity entity, ResourceLocation particleId) {
        return addTrackedEmitter(entity, particleId, true);
    }

    @Override
    public void removeAll() {
        super.removeAll();
        tracker.clear();
    }

    public static @Nullable ParticleEmitter getEmitter(int id) {
        if (screenManager != null && screenManager._getEmitter(id) instanceof ScreenParticleEmitter emitter) {
            return emitter;
        }
        return INSTANCE._getEmitter(id);
    }

    public static @Nullable Queue<IMolangParticleInstance> getParticlesForEmitter(ParticleEmitter emitter) {
        if (screenManager != null && emitter instanceof ScreenParticleEmitter) {
            return screenManager._getParticlesForEmitter(emitter);
        }
        return INSTANCE._getParticlesForEmitter(emitter);
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> PARTICLE_LISTER.listMatchingResources(resourceManager), backgroundExecutor).thenCompose(map -> {
            ModLoader.postEvent(new MolangParticleLoadEvent.Pre(backgroundExecutor));
            List<CompletableFuture<DefinedParticleEffect>> list = Lists.newArrayListWithExpectedSize(map.size());
            for (Map.Entry<ResourceLocation, Resource> entry : map.entrySet()) {
                list.add(CompletableFuture.supplyAsync(() -> {
                    try (Reader reader = entry.getValue().openAsReader()) {
                        return DefinedParticleEffect.CODEC.parse(JsonOps.INSTANCE, GsonHelper.parse(reader).get("particle_effect")).getOrThrow(JsonParseException::new);
                    } catch (IOException exception) {
                        ResourceLocation id = PARTICLE_LISTER.fileToId(entry.getKey());
                        throw new IllegalStateException("Failed to load definition for particle " + id, exception);
                    }
                }, backgroundExecutor));
            }
            return Util.sequence(list);
        }).thenCompose(preparationBarrier::wait).thenAcceptAsync(effects -> {
            ImmutableMap.Builder<ResourceLocation, DefinedParticleEffect> id2Effect = ImmutableMap.builder();
            ImmutableMap.Builder<ResourceLocation, ParticlePreset> id2Particle = ImmutableMap.builder();
            ImmutableMap.Builder<ResourceLocation, EmitterPreset> id2Emitter = ImmutableMap.builder();
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
            this.id2Effect = id2Effect.build();
            this.id2Particle = id2Particle.build();
            this.id2Emitter = id2Emitter.build();
            this.initialized = false;
            ModLoader.postEvent(new MolangParticleLoadEvent.Post(gameExecutor));
        }, gameExecutor);
    }
}

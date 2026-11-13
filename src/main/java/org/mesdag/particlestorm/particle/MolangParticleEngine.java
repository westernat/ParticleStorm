package org.mesdag.particlestorm.particle;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModLoader;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.PSClientConfigs;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.*;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.data.description.DescriptionMaterial;
import org.mesdag.particlestorm.data.description.DescriptionParameters;
import org.mesdag.particlestorm.data.description.ParticleDescription;
import org.mesdag.particlestorm.particle.screen.IParticleManagerScreen;
import org.mesdag.particlestorm.particle.screen.ScreenParticleEmitter;

import java.io.Reader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class MolangParticleEngine extends MolangParticleManager implements PreparableReloadListener {
    public static final MolangParticleEngine INSTANCE = new MolangParticleEngine();
    private static final FileToIdConverter PARTICLE_LISTER = FileToIdConverter.json("particle_definitions");
    private static final DefinedParticleEffect ERROR_EFFECT = new DefinedParticleEffect(new ParticleDescription(ParticleStorm.asResource("_"), new DescriptionParameters(DescriptionMaterial.NO_RENDER, ParticleStorm.asResource("_")), ParticleTypes.BLOCK), Map.of(), Map.of(), Map.of());
    private static MolangParticleManager screenManager;

    private Map<ResourceLocation, DefinedParticleEffect> id2Effect = Map.of();
    private Map<ResourceLocation, ParticlePreset> id2Particle = Map.of();
    private Map<ResourceLocation, EmitterPreset> id2Emitter = Map.of();

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

    public void saveEffect(DefinedParticleEffect effect) {
        ResourceLocation id = effect.description.identifier();
        ParticlePreset particlePreset = new ParticlePreset(effect);
        EmitterPreset emitterPreset = new EmitterPreset(effect.description.type(), effect.orderedEmitterComponents, effect.events);
        ClientLevel level = Minecraft.getInstance().level;
        for (IComponent component : effect.orderedComponents) {
            if (component instanceof IParticleComponent particleComponent) {
                particleComponent.initialize(level);
            }
        }
        Map<ResourceLocation, DefinedParticleEffect> effects = new HashMap<>(this.id2Effect);
        effects.put(id, effect);
        this.id2Effect = effects;
        Map<ResourceLocation, ParticlePreset> particles = new HashMap<>(this.id2Particle);
        particles.put(id, particlePreset);
        this.id2Particle = particles;
        Map<ResourceLocation, EmitterPreset> emitters = new HashMap<>(this.id2Emitter);
        emitters.put(id, emitterPreset);
        this.id2Emitter = emitters;
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

    public static @Nullable MolangParticleManager getScreenManager() {
        return screenManager;
    }

    @ApiStatus.Internal
    public static void _addParticle(IMolangParticleInstance instance) {
        if (getScreenManager() != null && instance.getEmitter() instanceof ScreenParticleEmitter) {
            getScreenManager().addParticle(instance);
        } else {
            INSTANCE.addParticle(instance);
        }
    }

    @ApiStatus.Internal
    public static void _addEmitter(ParticleEmitter emitter) {
        if (getScreenManager() != null && emitter instanceof ScreenParticleEmitter) {
            getScreenManager().addEmitter(emitter);
        } else {
            INSTANCE.addEmitter(emitter);
        }
    }

    @ApiStatus.Internal
    public static void _removeEmitter(int id) {
        if (getScreenManager() != null && getScreenManager().removeEmitter(id) instanceof ScreenParticleEmitter) {
            return;
        }
        INSTANCE.removeEmitter(id);
    }

    public boolean addTrackedEmitter(Entity entity, ResourceLocation particleId) {
        var queue = tracker.computeIfAbsent(entity, e -> new Object2ObjectLinkedOpenHashMap<>());
        if (!queue.isEmpty() && queue.containsKey(particleId)) return false;
        ParticleEmitter emitter = new ParticleEmitter(entity.level(), entity.position(), particleId);
        addEmitter(emitter);
        emitter.attachEntity(entity);
        queue.put(particleId, emitter);
        if (queue.size() > PSClientConfigs.maxTrackersPerEntity) {
            queue.removeFirst();
        }
        return true;
    }

    @Override
    public void removeAll() {
        super.removeAll();
        tracker.clear();
    }

    @ApiStatus.Internal
    public static @Nullable ParticleEmitter _getEmitter(int id) {
        if (getScreenManager() != null && getScreenManager().getEmitter(id) instanceof ScreenParticleEmitter emitter) {
            return emitter;
        }
        return INSTANCE.getEmitter(id);
    }

    @ApiStatus.Internal
    public static @Nullable Queue<IMolangParticleInstance> _getParticlesForEmitter(ParticleEmitter emitter) {
        if (getScreenManager() != null && emitter instanceof ScreenParticleEmitter) {
            return getScreenManager().getParticlesForEmitter(emitter);
        }
        return INSTANCE.getParticlesForEmitter(emitter);
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> PARTICLE_LISTER.listMatchingResources(resourceManager), backgroundExecutor).thenCompose(map -> {
            ModLoader.postEvent(new MolangParticleLoadEvent.Pre(backgroundExecutor));
            List<CompletableFuture<Pair<ResourceLocation, DefinedParticleEffect>>> list = Lists.newArrayListWithExpectedSize(map.size());
            for (Map.Entry<ResourceLocation, Resource> entry : map.entrySet()) {
                list.add(CompletableFuture.supplyAsync(() -> {
                    ResourceLocation file = PARTICLE_LISTER.fileToId(entry.getKey());
                    try (Reader reader = entry.getValue().openAsReader()) {
                        JsonElement particleEffect = GsonHelper.parse(reader).get("particle_effect");
                        return new Pair<>(file, DefinedParticleEffect.CODEC.parse(JsonOps.INSTANCE, particleEffect).getOrThrow(JsonParseException::new));
                    } catch (Exception e) {
                        ParticleStorm.LOGGER.error("Failed to load definition for particle {}, skipped", file, e);
                        return new Pair<>(file, ERROR_EFFECT);
                    }
                }, backgroundExecutor));
            }
            return Util.sequence(list);
        }).thenCompose(preparationBarrier::wait).thenAcceptAsync(pairs -> {
            Map<ResourceLocation, DefinedParticleEffect> id2Effect = new Object2ObjectOpenHashMap<>();
            Map<ResourceLocation, ParticlePreset> id2Particle = new Object2ObjectOpenHashMap<>();
            Map<ResourceLocation, EmitterPreset> id2Emitter = new Object2ObjectOpenHashMap<>();
            Map<ResourceLocation, List<ResourceLocation>> duplicated = new Object2ObjectOpenHashMap<>();
            for (Pair<ResourceLocation, DefinedParticleEffect> pair : pairs) {
                DefinedParticleEffect effect = pair.getSecond();
                if (effect == ERROR_EFFECT) continue;
                ResourceLocation file = pair.getFirst();
                ResourceLocation id = effect.description.identifier();
                try {
                    ParticlePreset pp = new ParticlePreset(effect);
                    EmitterPreset ep = new EmitterPreset(
                            effect.description.type(),
                            effect.orderedEmitterComponents,
                            effect.events
                    );
                    id2Effect.put(id, effect);
                    id2Particle.put(id, pp);
                    id2Emitter.put(id, ep);
                    duplicated.computeIfAbsent(id, rl -> new ArrayList<>()).add(file);
                } catch (Exception e) {
                    ParticleStorm.LOGGER.error("Error parsing particle, skipped. id={}, file={}", id, file, e);
                }
            }
            for (Map.Entry<ResourceLocation, List<ResourceLocation>> entry : duplicated.entrySet()) {
                List<ResourceLocation> files = entry.getValue();
                int length = files.size() - 1;
                if (length < 1) continue;
                ResourceLocation[] beReplaced = new ResourceLocation[length];
                for (int i = 0; i < beReplaced.length; i++) {
                    beReplaced[i] = files.get(i);
                }
                ParticleStorm.LOGGER.warn("Duplicated particle id {} found, files {} was replaced by file {}", entry.getKey(), Arrays.toString(beReplaced), files.getLast());
            }
            this.id2Effect = Collections.unmodifiableMap(id2Effect);
            this.id2Particle = Collections.unmodifiableMap(id2Particle);
            this.id2Emitter = Collections.unmodifiableMap(id2Emitter);
            this.initialized = false;
            ModLoader.postEvent(new MolangParticleLoadEvent.Post(gameExecutor));
        }, gameExecutor);
    }
}

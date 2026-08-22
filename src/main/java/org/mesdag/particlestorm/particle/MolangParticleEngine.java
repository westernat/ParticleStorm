package org.mesdag.particlestorm.particle;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.ParticleGroup;
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
import net.minecraftforge.fml.ModLoader;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.mesdag.particlestorm.PSClientConfigs;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.*;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.network.EmitterRemovalPacket;
import org.mesdag.particlestorm.network.EmitterSynchronizePacket;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

public final class MolangParticleEngine implements PreparableReloadListener {
    public static final MolangParticleEngine INSTANCE = new MolangParticleEngine();
    private static final FileToIdConverter PARTICLE_LISTER = FileToIdConverter.json("particle_definitions");
    private Map<ResourceLocation, DefinedParticleEffect> id2Effect = ImmutableMap.of();
    private Map<ResourceLocation, ParticlePreset> id2Particle = ImmutableMap.of();
    private Map<ResourceLocation, EmitterPreset> id2Emitter = ImmutableMap.of();

    private final Int2ObjectOpenHashMap<ParticleEmitter> emitters = new Int2ObjectOpenHashMap<>();
    private final Object2ObjectOpenCustomHashMap<Entity, Object2ObjectLinkedOpenHashMap<ResourceLocation, ParticleEmitter>> tracker = new Object2ObjectOpenCustomHashMap<>(new Hash.Strategy<>() {
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
    private final Queue<IMolangParticleInstance> particlesToAdd = new ArrayDeque<>();
    private final Reference2ObjectOpenHashMap<ParticleRenderType, Queue<IMolangParticleInstance>> groupedParticles = new Reference2ObjectOpenHashMap<>();
    private final IntAllocator allocator = new IntAllocator();
    private boolean initialized = false;

    private MolangParticleEngine() {}

    public Map<ResourceLocation, DefinedParticleEffect> id2Effect() {
        return id2Effect;
    }

    public Map<ResourceLocation, ParticlePreset> id2Particle() {
        return id2Particle;
    }

    public Map<ResourceLocation, EmitterPreset> id2Emitter() {
        return id2Emitter;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public void tick(Minecraft minecraft, LocalPlayer player) {
        if (!initialized) {
            for (ParticlePreset detail : id2Particle.values()) {
                for (IComponent component : detail.effect.orderedComponents) {
                    if (component instanceof IParticleComponent particleComponent) {
                        particleComponent.initialize(player.clientLevel);
                    }
                }
            }
            removeAll();
            this.initialized = true;
        }
        if (!emitters.isEmpty()) {
            int renderDistSqr = Mth.square(minecraft.options.renderDistance().get() * 16);
            var iterator = emitters.int2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                ParticleEmitter emitter = iterator.next().getValue();
                try {
                    if (emitter.isRemoved() || emitter.level.dimension() != player.clientLevel.dimension()) {
                        emitter.onRemove();
                        removeEmitterNoUpdate(emitter);
                        iterator.remove();
                    } else if (Mth.lengthSquared(
                            emitter.getPosition().x - player.getX(),
                            emitter.getPosition().z - player.getZ()
                    ) < renderDistSqr) {
                        emitter.tick();
                    }
                } catch (Throwable e) {
                    ParticleStorm.LOGGER.warn("Error ticking emitter: {}", e.getMessage());
                    e.printStackTrace();
                    if (emitter != null) {
                        removeEmitterNoUpdate(emitter);
                    }
                    iterator.remove();
                }
            }
        }
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
        if (!particlesToAdd.isEmpty()) {
            for (IMolangParticleInstance instance : particlesToAdd) {
                particlesForEmitter.computeIfAbsent(instance.getEmitter().id, i -> new ArrayDeque<>()).add(instance);
                groupedParticles.computeIfAbsent(instance.self().getRenderType(), o -> new ArrayDeque<>()).add(instance);
            }
            particlesToAdd.clear();
        }
        if (!groupedParticles.isEmpty()) {
            var iterator = groupedParticles.reference2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                iterator.next().getValue().removeIf(instance -> {
                    try {
                        instance.self().tick();
                        return instance.isDiscarded();
                    } catch (Throwable e) {
                        ParticleStorm.LOGGER.warn("Error ticking particle: {}", e.getMessage());
                        e.printStackTrace();
                        instance.discard();
                        return true;
                    }
                });
            }
        }
        if (!particlesForEmitter.isEmpty()) {
            var iterator = particlesForEmitter.int2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                iterator.next().getValue().removeIf(IMolangParticleInstance::isDiscarded);
            }
        }
    }

    public void renderParticles(LightTexture lightTexture, TextureManager textureManager, PoseStack poseStack, Camera camera, float partialTick, Frustum frustum, Predicate<ParticleRenderType> renderTypePredicate) {
        if (groupedParticles.isEmpty()) return;
        lightTexture.turnOnLightLayer();
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        if (cull) {
            RenderSystem.disableCull();
        }
        RenderSystem.enableDepthTest();
        RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE2);
        RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        PoseStack posestack = RenderSystem.getModelViewStack();
        posestack.pushPose();
        posestack.mulPoseMatrix(poseStack.last().pose());
        RenderSystem.applyModelViewMatrix();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();

        var iterator = groupedParticles.reference2ObjectEntrySet().fastIterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ParticleRenderType type = entry.getKey();
            if (type == ParticleRenderType.NO_RENDER || !renderTypePredicate.test(type)) continue;
            Queue<IMolangParticleInstance> queue = entry.getValue();
            if (queue.isEmpty()) continue;

            RenderSystem.setShader(!ParticleStorm.IRIS_LOADED && type == PSGameClient.PARTICLE_BLEND
                    ? PSGameClient::getParticleNoDiscardShader
                    : GameRenderer::getParticleShader);
            type.begin(builder, textureManager);

            for (IMolangParticleInstance instance : queue) {
                if (instance.isVisible(camera, frustum, partialTick)) {
                    try {
                        instance.self().render(builder, camera, partialTick);
                    } catch (Throwable e) {
                        CrashReport report = CrashReport.forThrowable(e, "Rendering Molang Particle");
                        CrashReportCategory category = report.addCategory("Molang Particle being rendered");
                        category.setDetail("Molang Particle Id", () -> instance.getEmitter().particleId.toString());
                        throw new ReportedException(report);
                    }
                }
            }
            type.end(tesselator);
        }

        posestack.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        lightTexture.turnOffLightLayer();
        if (cull) {
            RenderSystem.enableCull();
        }
    }

    public ObjectIterator<Int2ObjectMap.Entry<ParticleEmitter>> getEmitters() {
        return emitters.int2ObjectEntrySet().fastIterator();
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

    public void addEmitter(ParticleEmitter emitter) {
        addEmitter(emitter, false);
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

    public void addParticle(IMolangParticleInstance instance) {
        Optional<ParticleGroup> optional = instance.self().getParticleGroup();
        if (optional.isPresent()) {
            Queue<IMolangParticleInstance> queue = particlesForEmitter.get(instance.getEmitter().id);
            if (queue == null || queue.size() < optional.get().getLimit()) {
                particlesToAdd.add(instance);
            }
        } else {
            particlesToAdd.add(instance);
        }
    }

    public @Nullable Queue<IMolangParticleInstance> getParticlesForEmitter(ParticleEmitter emitter) {
        return particlesForEmitter.get(emitter.id);
    }

    private void removeEmitterNoUpdate(ParticleEmitter emitter) {
        emitter.remove();
        allocator.release(emitter.id);
        particlesForEmitter.remove(emitter.id);
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
        particlesForEmitter.remove(id);
        if (sync) EmitterRemovalPacket.sendToServer(id);
        return removed;
    }

    public void removeAll() {
        if (!emitters.isEmpty()) {
            ObjectIterator<Int2ObjectMap.Entry<ParticleEmitter>> iterator = emitters.int2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                iterator.next().getValue().remove();
                iterator.remove();
            }
        }
        tracker.clear();
        particlesForEmitter.clear();
        particlesToAdd.clear();
        groupedParticles.clear();
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
            ModLoader.get().postEvent(new MolangParticleLoadEvent.Pre(backgroundExecutor));
            List<CompletableFuture<DefinedParticleEffect>> list = Lists.newArrayListWithExpectedSize(map.size());
            for (Map.Entry<ResourceLocation, Resource> entry : map.entrySet()) {
                ResourceLocation id = PARTICLE_LISTER.fileToId(entry.getKey());
                list.add(CompletableFuture.supplyAsync(() -> {
                    try (Reader reader = entry.getValue().openAsReader()) {
                        return DefinedParticleEffect.CODEC.parse(JsonOps.INSTANCE, GsonHelper.parse(reader).get("particle_effect")).getOrThrow(false, msg -> {throw new JsonParseException(msg);});
                    } catch (IOException exception) {
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
            ModLoader.get().postEvent(new MolangParticleLoadEvent.Post(gameExecutor));
        }, gameExecutor);
    }
}

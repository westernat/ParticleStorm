package org.mesdag.particlestorm.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
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
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.IntAllocator;
import org.mesdag.particlestorm.api.RegisterCustomEmitterTypeEvent;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Predicate;

public class MolangParticleManager {
    /// 所有新建的发射器都先进这个队列，tick 开始时统一并入 `emitters`：
    /// tick 期间（事件节点里发射粒子）不会破坏正在遍历的 fastutil 迭代器，tick 外的添加也只是延迟一帧生效。
    protected final Queue<ParticleEmitter> emittersToAdd = new ArrayDeque<>();
    protected final Int2ObjectOpenHashMap<ParticleEmitter> emitters = new Int2ObjectOpenHashMap<>();
    protected final Int2ObjectOpenHashMap<Queue<IMolangParticleInstance>> particlesForEmitter = new Int2ObjectOpenHashMap<>();
    protected final Queue<IMolangParticleInstance> particlesToAdd = new ArrayDeque<>();
    protected final Reference2ObjectOpenHashMap<ParticleRenderType, Queue<IMolangParticleInstance>> groupedParticles = new Reference2ObjectOpenHashMap<>();
    public final IntAllocator allocator;

    public MolangParticleManager(IntAllocator allocator) {
        this.allocator = allocator;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public void tick(Minecraft minecraft, LocalPlayer player) {
        if (!emittersToAdd.isEmpty()) {
            ParticleEmitter emitter;
            while ((emitter = emittersToAdd.poll()) != null) {
                emitters.put(emitter.id, emitter);
            }
        }
        if (!emitters.isEmpty()) {
            int renderDistSqr = Mth.square(minecraft.options.renderDistance().get() * 16);
            // 本轮 tick 期间事件节点新加的发射器只会进 `emittersToAdd`，不会动 `emitters`，
            // 所以这里可以安全地边遍历边移除。
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

    public void addEmitter(ParticleEmitter emitter) {
        emitter.id = allocator.allocate();
        // 所有新建发射器都先进队列，tick 开始时统一并入 `emitters`
        emittersToAdd.add(emitter);
    }

    /// 丢弃还没并入的发射器（清场/重置时用，避免 id 泄漏）。
    private void discardPendingEmitters() {
        ParticleEmitter emitter;
        while ((emitter = emittersToAdd.poll()) != null) {
            emitter.onRemove();
            allocator.release(emitter.id);
        }
    }

    public void loadEmitter(Level level, int id, CompoundTag tag) {
        ParticleEmitter emitter = RegisterCustomEmitterTypeEvent.create(level, tag);
        emitter.id = id;
        emitters.put(id, emitter);
        if (allocator.forceAllocate(id)) {
            ParticleStorm.LOGGER.warn("There was an emitter exist before, now replaced");
        }
    }

    public @Nullable ParticleEmitter removeEmitter(int id) {
        ParticleEmitter removed = emitters.remove(id);
        if (removed != null) {
            removed.onRemove();
            allocator.release(id);
            particlesForEmitter.remove(id);
            return removed;
        }
        // 可能还在待加入队列里（新建发射器先入队、tick 开始才并入）
        Iterator<ParticleEmitter> iterator = this.emittersToAdd.iterator();
        while (iterator.hasNext()) {
            ParticleEmitter pending = iterator.next();
            if (pending.id != id) {
                continue;
            }
            iterator.remove();
            pending.onRemove();
            allocator.release(id);
            particlesForEmitter.remove(id);
            return pending;
        }
        return null;
    }

    /// 清空场景：移除全部发射器并回收 id，同时丢弃已生成与待生成的粒子队列（不动 allocator 本身）。
    @ApiStatus.Internal
    public void clearScene() {
        discardPendingEmitters();
        if (!emitters.isEmpty()) {
            ObjectIterator<Int2ObjectMap.Entry<ParticleEmitter>> iterator = emitters.int2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                ParticleEmitter emitter = iterator.next().getValue();
                emitter.onRemove();
                allocator.release(emitter.id);
                iterator.remove();
            }
        }
        particlesForEmitter.clear();
        particlesToAdd.clear();
        groupedParticles.clear();
    }

    private void removeEmitterNoUpdate(ParticleEmitter emitter) {
        emitter.remove();
        allocator.release(emitter.id);
        particlesForEmitter.remove(emitter.id);
    }

    public void removeAll() {
        discardPendingEmitters();
        if (!emitters.isEmpty()) {
            ObjectIterator<Int2ObjectMap.Entry<ParticleEmitter>> iterator = emitters.int2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                iterator.next().getValue().remove();
                iterator.remove();
            }
        }
        particlesForEmitter.clear();
        particlesToAdd.clear();
        groupedParticles.clear();
        allocator.clear();
    }

    public boolean contains(int id) {
        return allocator.isAllocated(id);
    }

    public @Nullable ParticleEmitter getEmitter(int id) {
        ParticleEmitter emitter = emitters.get(id);
        if (emitter != null) {
            return emitter;
        }
        for (ParticleEmitter pending : emittersToAdd) {
            if (pending.id == id) {
                return pending;
            }
        }
        return null;
    }

    public @Nullable Queue<IMolangParticleInstance> getParticlesForEmitter(ParticleEmitter emitter) {
        return particlesForEmitter.get(emitter.id);
    }

    public ObjectIterator<Int2ObjectMap.Entry<ParticleEmitter>> getEmitters() {
        return emitters.int2ObjectEntrySet().fastIterator();
    }

    /// 还没被 tick 收编进 `emitters` 的第一个发射器（暂停时不会 tick，发射器会一直停在待加入队列里）。
    @ApiStatus.Internal
    public @Nullable ParticleEmitter peekPendingEmitter() {
        return emittersToAdd.peek();
    }

    public int totalEmitterCount() {
        return emitters.size() + emittersToAdd.size();
    }

    public int totalParticleCount() {
        return particlesForEmitter.values().stream().mapToInt(Queue::size).sum();
    }

    public void renderParticles(LightTexture lightTexture, TextureManager textureManager, Camera camera, float partialTick, Frustum frustum, Predicate<ParticleRenderType> renderTypePredicate) {
        if (groupedParticles.isEmpty()) return;
        if (RenderSystem.getShaderTexture(2) == 0) {
            lightTexture.turnOnLightLayer();
        }
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        if (cull) {
            RenderSystem.disableCull();
        }
        Tesselator tesselator = Tesselator.getInstance();
        var iterator = groupedParticles.reference2ObjectEntrySet().fastIterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ParticleRenderType type = entry.getKey();
            if (type == ParticleRenderType.NO_RENDER || !renderTypePredicate.test(type)) continue;
            Queue<IMolangParticleInstance> queue = entry.getValue();
            if (queue.isEmpty()) continue;

            BufferBuilder builder = type.begin(tesselator, textureManager);
            RenderSystem.setShader(!ParticleStorm.IRIS_LOADED && type.isTranslucent()
                    ? PSGameClient::getParticleNoDiscardShader
                    : GameRenderer::getParticleShader);
            if (builder == null) continue;

            for (Iterator<IMolangParticleInstance> ordered = orderForRender(type, queue, camera, partialTick); ordered.hasNext(); ) {
                renderInstance(ordered.next(), builder, camera, frustum, partialTick);
            }

            MeshData mesh = builder.build();
            if (mesh != null) {
                BufferUploader.drawWithShader(mesh);
            }
        }
        if (cull) {
            RenderSystem.enableCull();
        }
    }

    /// 决定某个渲染类型粒子的绘制顺序。默认返回队列自身的迭代器（世界渲染保持原版行为）。
    ///
    /// 编辑器预览会覆写为“按相机距离从远到近”，因为 alpha 混合有顺序要求，
    /// 否则后生成的“远处粒子”会盖在“近处粒子”上面，看起来像穿透。
    protected Iterator<IMolangParticleInstance> orderForRender(ParticleRenderType type, Queue<IMolangParticleInstance> queue, Camera camera, float partialTick) {
        return queue.iterator();
    }

    private void renderInstance(IMolangParticleInstance instance, BufferBuilder builder, Camera camera, Frustum frustum, float partialTick) {
        if (!instance.isVisible(camera, frustum, partialTick)) {
            return;
        }
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

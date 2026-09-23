package org.mesdag.particlestorm.api.geckolib;

import org.mesdag.particlestorm.particle.MolangParticleEngine;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.state.KeyFrameEvent;
import com.geckolib.cache.animation.keyframeevent.ParticleKeyframeData;
import com.geckolib.cache.model.GeoLocator;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4x3f;
import net.neoforged.fml.ModLoader;
import org.mesdag.particlestorm.PSDiagnostics;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.api.ParticleEmitterAttachable;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class GeckoLibHelper {
    private static final Map<AnimatableManager<?>, Map<LocatorParticleKey, BoundEmitter>> LOCATOR_EMITTERS = new WeakHashMap<>();
    private static final Map<RenderPassInfo<?>, Map<GeoLocator, List<BoundEmitter>>> ACTIVE_LOCATOR_EMITTERS = new WeakHashMap<>();
    private static final ThreadLocal<RenderPassInfo<?>> CURRENT_RENDER_PASS = new ThreadLocal<>();
    private static final List<Runnable> RELOAD_CALLBACKS = new ArrayList<>();

    private GeckoLibHelper() {
    }

    public static void postEvent() {
        ModLoader.postEvent(new RegisterLocatorPreTransformerEvent());
    }

    public static void addReloadCallback(Runnable callback) {
        RELOAD_CALLBACKS.add(callback);
    }

    public static void clearReloadCallbacks() {
        RELOAD_CALLBACKS.clear();
        clearLocatorBindings();
    }

    public static void afterReload() {
        clearLocatorBindings();
        for (Runnable callback : List.copyOf(RELOAD_CALLBACKS)) {
            callback.run();
        }
    }

    private static void clearLocatorBindings() {
        LOCATOR_EMITTERS.clear();
        ACTIVE_LOCATOR_EMITTERS.clear();
        CURRENT_RENDER_PASS.remove();
    }

    public static void processParticleEffect(KeyFrameEvent<? extends GeoAnimatable, ParticleKeyframeData> event) {
        try {
            ParticleContext context = createContext(event.animatable(), event.renderState());
            if (context == null) {
                return;
            }

            Identifier particleId = Identifier.parse(event.keyframeData().getEffect());
            String locator = event.keyframeData().getLocatorName();
            ParticleEmitter emitter = getOrCreateEmitter(event.renderState(), context, locator, particleId, event.animatable());
            if (emitter == null) {
                return;
            }

            if (locator == null || locator.isBlank()) {
                emitter.setPos(context.basePos());
            }
        } catch (RuntimeException exception) {
            PSDiagnostics.warnOnce("geckolib-particle-keyframe:" + event.keyframeData().getEffect(), "GeckoLib particle keyframe failed effect={} locator={} error={}",
                    event.keyframeData().getEffect(),
                    event.keyframeData().getLocatorName(),
                    exception.getMessage()
            );
        }
    }

    public static void attachLocatorListeners(GeoRenderState renderState, RenderPassInfo<?> renderPassInfo) {
        AnimatableManager<?> manager = renderState.getGeckolibData(DataTickets.ANIMATABLE_MANAGER);
        if (manager == null) {
            return;
        }

        Map<LocatorParticleKey, BoundEmitter> emitters = LOCATOR_EMITTERS.get(manager);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<LocatorParticleKey, BoundEmitter>> iterator = emitters.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LocatorParticleKey, BoundEmitter> entry = iterator.next();
            BoundEmitter bound = entry.getValue();
            ParticleEmitter emitter = MolangParticleEngine.INSTANCE.getEmitter(bound.emitterId());
            if (emitter == null || emitter.isRemoved()) {
                iterator.remove();
                continue;
            }

            attachLocatorListener(renderPassInfo, bound, emitter);
        }
    }

    public static void enterRenderPass(RenderPassInfo<?> renderPassInfo) {
        CURRENT_RENDER_PASS.set(renderPassInfo);
    }

    public static void exitRenderPass(RenderPassInfo<?> renderPassInfo) {
        if (CURRENT_RENDER_PASS.get() == renderPassInfo) {
            CURRENT_RENDER_PASS.remove();
        }
        ACTIVE_LOCATOR_EMITTERS.remove(renderPassInfo);
    }

    public static void removeEmitters(GeoRenderState renderState) {
        AnimatableManager<?> manager = renderState.getGeckolibData(DataTickets.ANIMATABLE_MANAGER);
        if (manager == null) {
            return;
        }

        Map<LocatorParticleKey, BoundEmitter> emitters = LOCATOR_EMITTERS.remove(manager);
        if (emitters != null) {
            for (BoundEmitter bound : emitters.values()) {
                MolangParticleEngine.INSTANCE.removeEmitter(bound.emitterId(), false);
            }
        }
    }

    public static void setCurrentEntity(Object animatable, @Nullable Entity entity) {
        if (animatable instanceof WithCurrentEntity withCurrentEntity) {
            withCurrentEntity.setCurrentEntity(entity);
        }
    }

    private static @Nullable ParticleEmitter getOrCreateEmitter(GeoRenderState renderState, ParticleContext context, @Nullable String locator, Identifier particleId, GeoAnimatable animatable) {
        if (locator == null || locator.isBlank()) {
            ParticleEmitter emitter = new ParticleEmitter(context.level(), context.basePos(), particleId, MolangExp.EMPTY);
            MolangParticleEngine.INSTANCE.addEmitter(emitter, false);
            attachContext(emitter, context);
            return emitter;
        }

        AnimatableManager<?> manager = renderState.getGeckolibData(DataTickets.ANIMATABLE_MANAGER);
        if (manager == null) {
            return null;
        }

        Map<LocatorParticleKey, BoundEmitter> emitters = LOCATOR_EMITTERS.computeIfAbsent(manager, ignored -> new Object2ObjectOpenHashMap<>());
        LocatorParticleKey key = new LocatorParticleKey(locator, particleId);
        BoundEmitter bound = emitters.get(key);
        ParticleEmitter current = bound == null ? null : MolangParticleEngine.INSTANCE.getEmitter(bound.emitterId());
        if (current != null && !current.isRemoved() && particleId.equals(current.particleId) && current.level == context.level()) {
            attachCurrentRenderPassListener(bound, current);
            return current;
        }

        if (current != null) {
            MolangParticleEngine.INSTANCE.removeEmitter(current, false);
        }

        ParticleEmitter emitter = new ParticleEmitter(context.level(), context.basePos(), particleId, MolangExp.EMPTY);
        MolangParticleEngine.INSTANCE.addEmitter(emitter, false);
        attachContext(emitter, context);
        emitter.parentSpace = new Matrix4x3f();
        BoundEmitter newBound = new BoundEmitter(emitter.id, locator, particleId, context.basePos(), animatable);
        emitters.put(key, newBound);
        attachCurrentRenderPassListener(newBound, emitter);
        PSDiagnostics.infoFirstN("geckolib-emitter-create:" + locator + ":" + particleId, 8, "GeckoLib locator emitter created runtimeId={} particle={} locator={} basePos={}",
                emitter.id,
                particleId,
                locator,
                context.basePos()
        );
        return emitter;
    }

    private static void attachContext(ParticleEmitter emitter, ParticleContext context) {
        if (context.attachable() != null) {
            emitter.attach(context.attachable());
        }
    }

    private static void updateEmitterPosition(ParticleEmitter emitter, BoundEmitter bound, @Nullable Vec3 worldPos, @Nullable Vec3 modelPos, @Nullable Vec3 localPos) {
        if (RegisterLocatorPreTransformerEvent.getRegisteredTransformer(bound.animatable()) != null) {
            return;
        }
        Vec3 target = worldPos != null ? worldPos : modelPos != null ? bound.basePos().add(modelPos.scale(1.0 / 16.0)) : localPos;
        if (target == null) {
            PSDiagnostics.warnFirstN("geckolib-locator-missing:" + bound.locator() + ":" + bound.particleId(), 8, "GeckoLib locator update missing position runtimeId={} particle={} locator={}",
                    emitter.id,
                    bound.particleId(),
                    bound.locator()
            );
            return;
        }

        emitter.setPos(target);
        PSDiagnostics.infoFirstN("geckolib-locator-update:" + bound.locator() + ":" + bound.particleId(), 8, "GeckoLib locator update runtimeId={} particle={} locator={} target={} worldPos={} modelPos={} localPos={}",
                emitter.id,
                bound.particleId(),
                bound.locator(),
                target,
                worldPos,
                modelPos,
                localPos
        );
    }

    private static void attachCurrentRenderPassListener(BoundEmitter bound, ParticleEmitter emitter) {
        RenderPassInfo<?> renderPassInfo = CURRENT_RENDER_PASS.get();
        if (renderPassInfo != null) {
            attachLocatorListener(renderPassInfo, bound, emitter);
        }
    }

    private static void attachLocatorListener(RenderPassInfo<?> renderPassInfo, BoundEmitter bound, ParticleEmitter emitter) {
        renderPassInfo.model().getLocator(bound.locator()).ifPresent(locator -> {
            List<BoundEmitter> bounds = ACTIVE_LOCATOR_EMITTERS
                    .computeIfAbsent(renderPassInfo, ignored -> new Object2ObjectOpenHashMap<>())
                    .computeIfAbsent(locator, ignored -> new ArrayList<>());
            if (!bounds.contains(bound)) {
                bounds.add(bound);
            }
            renderPassInfo.addLocatorPositionListener(bound.locator(), (worldPos, modelPos, localPos) -> updateEmitterPosition(emitter, bound, worldPos, modelPos, localPos));
        });
    }

    public static void captureLocatorTransform(GeoLocator locator, PoseStack poseStack, RenderPassInfo<?> renderPassInfo) {
        Map<GeoLocator, List<BoundEmitter>> locators = ACTIVE_LOCATOR_EMITTERS.get(renderPassInfo);
        if (locators == null) {
            return;
        }
        List<BoundEmitter> bounds = locators.get(locator);
        if (bounds == null || bounds.isEmpty()) {
            return;
        }

        Matrix4f localPose = RenderUtil.extractPoseFromRoot(new Matrix4f(poseStack.last().pose()), renderPassInfo.getPreRenderMatrixState());
        for (BoundEmitter bound : bounds) {
            ParticleEmitter emitter = MolangParticleEngine.INSTANCE.getEmitter(bound.emitterId());
            if (emitter == null || emitter.isRemoved()) {
                continue;
            }
            Matrix4x3f transform = new Matrix4x3f();
            RegisterLocatorPreTransformerEvent.Transformer<GeoAnimatable> transformer =
                    RegisterLocatorPreTransformerEvent.getRegisteredTransformer(bound.animatable());
            if (transformer == null) {
                localPose.get4x3(transform);
            } else {
                transformer.transform(locator.parent(), bound.animatable(), transform, renderPassInfo.renderState().getPartialTick());
                transform.translate(locator.offsetX() / 16.0F, locator.offsetY() / 16.0F, locator.offsetZ() / 16.0F)
                        .rotateZ(locator.rotZ()).rotateY(locator.rotY()).rotateX(locator.rotX());
            }
            emitter.setLocalSpace(transform);
        }
    }

    private static @Nullable ParticleContext createContext(GeoAnimatable animatable, GeoRenderState renderState) {
        Object target = animatable instanceof WithCurrentEntity withCurrentEntity && withCurrentEntity.getCurrentEntity() != null
                ? withCurrentEntity.getCurrentEntity() : animatable;
        ParticleEmitterAttachable attachable = target instanceof ParticleEmitterAttachable value ? value : null;
        if (attachable != null && attachable.getLevel() == null) {
            return null;
        }
        Level level;
        Vec3 basePos;
        VariableTable variableTable;

        if (attachable != null && attachable.getLevel() != null) {
            level = attachable.getLevel();
            basePos = attachable.getPos();
            variableTable = attachable.getVariableTable();
        } else if (Minecraft.getInstance().level != null) {
            level = Minecraft.getInstance().level;
            basePos = renderState.getOrDefaultGeckolibData(DataTickets.POSITION, Vec3.ZERO);
            variableTable = new VariableTable(null);
        } else {
            return null;
        }

        if (basePos == Vec3.ZERO) {
            PSDiagnostics.infoOnce("geckolib-no-render-position:" + animatable.getClass().getName(), "GeckoLib particle keyframe has no render position for {}", animatable);
        }
        return new ParticleContext(level, basePos, variableTable, attachable);
    }

    private record ParticleContext(Level level, Vec3 basePos, VariableTable variableTable, @Nullable ParticleEmitterAttachable attachable) {
    }

    private record LocatorParticleKey(String locator, Identifier particleId) {
    }

    private record BoundEmitter(int emitterId, String locator, Identifier particleId, Vec3 basePos, GeoAnimatable animatable) {
    }
}

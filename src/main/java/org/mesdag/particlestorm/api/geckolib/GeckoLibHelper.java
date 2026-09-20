package org.mesdag.particlestorm.api.geckolib;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.mesdag.particlestorm.PSDiagnostics;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.mixed.IPSAnimationController;
import org.mesdag.particlestorm.mixed.IPSGeoBone;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.mesdag.particlestorm.particle.ParticleEmitter;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.keyframe.event.data.ParticleKeyframeData;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class GeckoLibHelper {
    private static final Map<AnimatableInstanceCache, Map<LocatorParticleKey, BoundEmitter>> LOCATOR_EMITTERS = new WeakHashMap<>();

    private GeckoLibHelper() {}

    public static void processParticleEffect(@Nullable GeoAnimatable animatable, AnimationController<?> controller, ParticleKeyframeData keyframeData) {
        try {
            if (animatable == null) return;
            Entity entity = animatable instanceof Entity value ? value
                    : animatable instanceof ParticleStormGeoReplacedEntity replaced ? replaced.getCurrentEntity() : null;
            BlockEntity blockEntity = animatable instanceof BlockEntity value ? value : null;
            Level level = entity != null ? entity.level() : blockEntity != null ? blockEntity.getLevel() : Minecraft.getInstance().level;
            if (level == null) return;
            Vec3 basePos = entity != null ? entity.position() : blockEntity != null ? Vec3.atBottomCenterOf(blockEntity.getBlockPos()) : Vec3.ZERO;
            ResourceLocation particleId = new ResourceLocation(keyframeData.getEffect());
            String locatorName = keyframeData.getLocator();
            if (locatorName == null || locatorName.isBlank()) {
                createEmitter(level, basePos, particleId, entity, blockEntity);
                return;
            }

            Map<LocatorParticleKey, BoundEmitter> bindings = LOCATOR_EMITTERS.computeIfAbsent(animatable.getAnimatableInstanceCache(), ignored -> new HashMap<>());
            LocatorParticleKey key = new LocatorParticleKey(locatorName, particleId);
            BoundEmitter current = bindings.get(key);
            if (current != null && isActive(current.emitter())) return;
            for (GeoBone bone : IPSAnimationController.of(controller).particlestorm$getBonesWhichHasLocators()) {
                Map<String, LocatorValue> locators = IPSGeoBone.of(bone).particlestorm$getLocators();
                LocatorValue locator = locators == null ? null : locators.get(locatorName);
                if (locator == null) continue;
                ParticleEmitter emitter = createEmitter(level, basePos, particleId, entity, blockEntity);
                emitter.parentSpace = new Matrix4f();
                bindings.put(key, new BoundEmitter(emitter, bone, locator));
                return;
            }
        } catch (RuntimeException exception) {
            PSDiagnostics.warnOnce("geckolib-particle-keyframe:" + keyframeData.getEffect(), "GeckoLib particle keyframe failed effect={} locator={} error={}",
                    keyframeData.getEffect(), keyframeData.getLocator(), exception.getMessage());
        }
    }

    private static ParticleEmitter createEmitter(Level level, Vec3 pos, ResourceLocation particleId, @Nullable Entity entity, @Nullable BlockEntity blockEntity) {
        ParticleEmitter emitter = new ParticleEmitter(level, pos, particleId, MolangExp.EMPTY);
        MolangParticleEngine.INSTANCE.addEmitter(emitter);
        if (entity != null) {
            emitter.attachEntity(entity);
        } else if (blockEntity != null) {
            emitter.attachedBlock = blockEntity;
        }
        return emitter;
    }

    private static boolean isActive(ParticleEmitter emitter) {
        return !emitter.isRemoved() && MolangParticleEngine.INSTANCE.getEmitter(emitter.id) == emitter;
    }

    public static void setCurrentEntity(GeoAnimatable animatable, @Nullable Entity entity) {
        if (animatable instanceof ParticleStormGeoReplacedEntity replaced) {
            replaced.setCurrentEntity(entity);
        }
    }

    public static void removeEmittersWhenAnimationChange(AnimationController.State animationState, AnimatableInstanceCache cache) {
        if (animationState != AnimationController.State.TRANSITIONING) return;
        Map<LocatorParticleKey, BoundEmitter> bindings = LOCATOR_EMITTERS.remove(cache);
        if (bindings != null) {
            for (BoundEmitter bound : bindings.values()) {
                if (isActive(bound.emitter())) MolangParticleEngine.INSTANCE.removeEmitter(bound.emitter(), false);
            }
        }
    }

    public static void transformLocator(GeoBone bone, GeoAnimatable animatable, float partialTick) {
        Map<LocatorParticleKey, BoundEmitter> bindings = LOCATOR_EMITTERS.get(animatable.getAnimatableInstanceCache());
        if (bindings == null || bindings.isEmpty()) return;
        bindings.values().removeIf(bound -> !isActive(bound.emitter()));
        PoseStack poseStack = new PoseStack();
        RegisterLocatorPreTransformerEvent.getTransformer(animatable).transform(bone, animatable, poseStack, partialTick);
        for (BoundEmitter bound : bindings.values()) {
            if (bound.bone() != bone) continue;
            LocatorValue locator = bound.locator();
            double[] offset = locator.locatorClass() == null ? locator.values() : locator.locatorClass().offset();
            double[] rotation = locator.locatorClass() == null ? new double[3] : locator.locatorClass().rotation();
            poseStack.pushPose();
            poseStack.mulPose(new Quaternionf().rotationXYZ(
                    (float) Math.toRadians(rotation[0]), (float) Math.toRadians(rotation[1]), (float) Math.toRadians(rotation[2])));
            poseStack.translate(-offset[0] / 16.0, offset[1] / 16.0, offset[2] / 16.0);
            ParticleEmitter emitter = bound.emitter();
            emitter.parentSpace.set(poseStack.last().pose());
            Vec3 basePos = emitter.getAttachedEntity() != null ? emitter.getAttachedEntity().position()
                    : emitter.attachedBlock != null ? Vec3.atBottomCenterOf(emitter.attachedBlock.getBlockPos()) : Vec3.ZERO;
            emitter.setPos(basePos.add(emitter.parentSpace.m30(), emitter.parentSpace.m31(), emitter.parentSpace.m32()));
            poseStack.popPose();
        }
    }

    private record LocatorParticleKey(String locator, ResourceLocation particleId) {}
    private record BoundEmitter(ParticleEmitter emitter, GeoBone bone, LocatorValue locator) {}
}

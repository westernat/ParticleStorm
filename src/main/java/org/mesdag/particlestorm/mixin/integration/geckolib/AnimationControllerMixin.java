package org.mesdag.particlestorm.mixin.integration.geckolib;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import org.mesdag.particlestorm.mixed.IAnimationController;
import org.mesdag.particlestorm.mixed.IGeoBone;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.keyframe.event.data.ParticleKeyframeData;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.animation.AnimationController", remap = false)
public abstract class AnimationControllerMixin<T extends GeoAnimatable> implements IAnimationController {
    @Shadow
    @Final
    protected T animatable;
    @Shadow
    protected AnimationController.State animationState;

    @Unique
    private List<GeoBone> particlestorm$bonesWhichHasLocators;

    @Override
    public List<GeoBone> particlestorm$getBonesWhichHasLocators() {
        return Objects.requireNonNullElse(particlestorm$bonesWhichHasLocators, List.of());
    }

    @Override
    public void particlestorm$setBonesWhichHasLocators(Collection<GeoBone> registeredBones) {
        if (particlestorm$bonesWhichHasLocators == null) {
            this.particlestorm$bonesWhichHasLocators = registeredBones.stream().filter(bone -> {
                Map<String, LocatorValue> locators = IGeoBone.of(bone).particlestorm$getLocators();
                return locators != null && !locators.isEmpty();
            }).toList();
        }
    }

    @WrapWithCondition(method = "processCurrentAnimation", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;log(Lorg/apache/logging/log4j/Level;Ljava/lang/String;)V", ordinal = 1))
    private boolean processParticleEffect(Logger instance, Level level, String s, @Local(argsOnly = true, ordinal = 0) double adjustedTick, @Local ParticleKeyframeData keyframeData) {
        return GeckoLibHelper.processParticleEffect(animatable, this, keyframeData);
    }

    @Inject(method = "resetEventKeyFrames", at = @At("HEAD"))
    private void removeEmitters(CallbackInfo ci) {
        if (!particlestorm$getBonesWhichHasLocators().isEmpty()) {
            GeckoLibHelper.removeEmittersWhenAnimationChange(animationState, animatable.getAnimatableInstanceCache());
        }
    }
}

package org.mesdag.particlestorm.mixin.integration.geckolib;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.mesdag.particlestorm.integration.geckolib.GeckoLibHelper;
import org.mesdag.particlestorm.mixed.IAnimationController;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.keyframe.event.ParticleKeyframeEvent;
import software.bernie.geckolib.animation.keyframe.event.data.KeyFrameData;
import software.bernie.geckolib.animation.keyframe.event.data.ParticleKeyframeData;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.List;
import java.util.Set;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.animation.AnimationController", remap = false)
public abstract class AnimationControllerMixin<T extends GeoAnimatable> implements IAnimationController {
    @Shadow
    @Final
    protected T animatable;
    @Shadow
    @Final
    private Set<KeyFrameData> executedKeyFrames;
    @Shadow
    protected AnimationController.State animationState;

    @Unique
    private List<GeoBone> particlestorm$bonesWhichHasLocators;

    @Override
    public List<GeoBone> particlestorm$getBonesWhichHasLocators() {
        return particlestorm$bonesWhichHasLocators;
    }

    @Override
    public void particlestorm$setBonesWhichHasLocators(List<GeoBone> bones) {
        this.particlestorm$bonesWhichHasLocators = bones;
    }

    @WrapWithCondition(method = "processCurrentAnimation", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;log(Lorg/apache/logging/log4j/Level;Ljava/lang/String;)V", ordinal = 1))
    private boolean processParticleEffect(Logger instance, Level level, String s, @Local(argsOnly = true, ordinal = 0) double adjustedTick, @Local ParticleKeyframeData keyframeData) {
        return GeckoLibHelper.processParticleEffect(new ParticleKeyframeEvent<>(animatable, adjustedTick, (AnimationController<T>) (Object) this, keyframeData));
    }

    @Inject(method = "resetEventKeyFrames", at = @At("HEAD"))
    private void removeEmitters(CallbackInfo ci) {
        if (particlestorm$bonesWhichHasLocators != null) {
            GeckoLibHelper.removeEmittersWhenAnimationChange(particlestorm$bonesWhichHasLocators.size(), animationState, executedKeyFrames);
        }
    }
}

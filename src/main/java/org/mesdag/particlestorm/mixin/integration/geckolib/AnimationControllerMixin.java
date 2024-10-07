package org.mesdag.particlestorm.mixin.integration.geckolib;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.mesdag.particlestorm.integration.geckolib.GeckoLibHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.keyframe.event.ParticleKeyframeEvent;
import software.bernie.geckolib.animation.keyframe.event.data.ParticleKeyframeData;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.animation.AnimationController", remap = false)
public abstract class AnimationControllerMixin<T extends GeoAnimatable> {
    @Shadow
    @Final
    protected T animatable;

    @WrapWithCondition(method = "processCurrentAnimation", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;log(Lorg/apache/logging/log4j/Level;Ljava/lang/String;)V", ordinal = 1))
    private boolean processParticleEffect(Logger instance, Level level, String s, @Local(argsOnly = true, ordinal = 0) double adjustedTick, @Local ParticleKeyframeData keyframeData) {
        return GeckoLibHelper.processParticleEffect(new ParticleKeyframeEvent<>(animatable, adjustedTick, (AnimationController<T>) (Object) this, keyframeData));
    }
}

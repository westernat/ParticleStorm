package org.mesdag.particlestorm.mixin.integration.geckolib;

import com.llamalad7.mixinextras.sugar.Local;
import org.mesdag.particlestorm.mixed.IPSAnimationController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.Collection;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.animation.AnimationProcessor", remap = false)
public abstract class AnimationProcessorMixin<T extends GeoAnimatable> {
    @Shadow
    public abstract Collection<GeoBone> getRegisteredBones();

    @Inject(method = "tickAnimation", at = @At(value = "INVOKE", target = "Lsoftware/bernie/geckolib/animation/AnimationController;process(Lsoftware/bernie/geckolib/model/GeoModel;Lsoftware/bernie/geckolib/animation/AnimationState;Ljava/util/Map;Ljava/util/Map;DZ)V"))
    private void fillLocators(CallbackInfo ci, @Local(name = "controller") AnimationController<T> controller) {
        IPSAnimationController.of(controller).particlestorm$setBonesWhichHasLocators(getRegisteredBones());
    }
}

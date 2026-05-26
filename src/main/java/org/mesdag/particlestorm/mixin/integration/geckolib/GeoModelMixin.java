package org.mesdag.particlestorm.mixin.integration.geckolib;

import com.llamalad7.mixinextras.sugar.Local;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.cache.object.GeoBone;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.model.GeoModel")
public abstract class GeoModelMixin<T extends GeoAnimatable> {
    @Inject(method = "handleAnimations", at = @At("TAIL"))
    private void transform(
            CallbackInfo ci,
            @Local(argsOnly = true) T animatable,
            @Local(argsOnly = true) float partialTick,
            @Local(name = "processor") AnimationProcessor<T> processor
    ) {
        for (GeoBone bone : processor.getRegisteredBones()) {
            GeckoLibHelper.transformLocator(bone, animatable, partialTick);
        }
    }
}

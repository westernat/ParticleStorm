package org.mesdag.particlestorm.mixin.integration.geckolib;

import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.cache.object.GeoBone;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.model.GeoModel", remap = false)
public abstract class GeoModelMixin<T extends GeoAnimatable> {
    @Shadow
    public abstract AnimationProcessor<T> getAnimationProcessor();

    @Inject(method = "handleAnimations", at = @At("TAIL"))
    private void transform(T animatable, long instanceId, AnimationState<T> state, CallbackInfo ci) {
        for (CoreGeoBone bone : getAnimationProcessor().getRegisteredBones()) {
            if (bone instanceof GeoBone geoBone) {
                GeckoLibHelper.transformLocator(geoBone, animatable, state.getPartialTick());
            }
        }
    }
}

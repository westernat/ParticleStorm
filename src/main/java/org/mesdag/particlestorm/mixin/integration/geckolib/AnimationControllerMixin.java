package org.mesdag.particlestorm.mixin.integration.geckolib;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.cache.animation.keyframeevent.ParticleKeyframeData;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import org.mesdag.particlestorm.PSDiagnostics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.geckolib.animation.AnimationController", remap = false)
public abstract class AnimationControllerMixin<T extends GeoAnimatable> {
    @Shadow
    protected @Nullable AnimationController.KeyframeEventHandler<T, ParticleKeyframeData> particleKeyframeHandler;
    @Shadow
    @Final
    protected String name;

    @Unique
    private AnimationController.KeyframeEventHandler<T, ParticleKeyframeData> particlestorm$wrappedParticleHandler;

    @Inject(method = "checkControllerState", at = @At("HEAD"))
    private void particlestorm$ensureParticleHandler(T animatable, GeoRenderState renderState, AnimatableManager<T> manager, GeoModel<T> geoModel, CallbackInfoReturnable<Boolean> cir) {
        particlestorm$wrapParticleHandler();
    }

    @Inject(method = "setParticleKeyframeHandler", at = @At("TAIL"))
    private void particlestorm$wrapCustomParticleHandler(AnimationController.KeyframeEventHandler<T, ParticleKeyframeData> particleHandler, CallbackInfoReturnable<AnimationController<T>> cir) {
        particlestorm$wrapParticleHandler();
    }

    @Inject(method = "initializeNewAnimation", at = @At("HEAD"))
    private void particlestorm$removeEmitterOnNewAnimation(T animatable, GeoRenderState renderState, GeoModel<T> geoModel, double prevAnimSpeed, int prevTransitionTicks, CallbackInfo ci) {
        GeckoLibHelper.removeEmitters(renderState);
    }

    @Unique
    private void particlestorm$wrapParticleHandler() {
        if (particleKeyframeHandler == particlestorm$wrappedParticleHandler) {
            return;
        }

        AnimationController.KeyframeEventHandler<T, ParticleKeyframeData> original = particleKeyframeHandler;
        particlestorm$wrappedParticleHandler = event -> {
            if (original != null) {
                original.handle(event);
            }
        };
        particleKeyframeHandler = particlestorm$wrappedParticleHandler;
        PSDiagnostics.infoOnce("geckolib-animation-controller:" + name, "GeckoLib AnimationController particle handler hooked controller={}", name);
    }
}

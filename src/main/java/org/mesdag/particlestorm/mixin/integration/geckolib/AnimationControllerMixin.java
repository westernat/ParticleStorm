package org.mesdag.particlestorm.mixin.integration.geckolib;

import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import org.mesdag.particlestorm.mixed.IPSAnimationController;
import org.mesdag.particlestorm.mixed.IPSGeoBone;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.keyframe.event.data.ParticleKeyframeData;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.core.animation.AnimationController", remap = false)
public abstract class AnimationControllerMixin<T extends GeoAnimatable> implements IPSAnimationController {
    @Shadow
    @Final
    protected T animatable;
    @Shadow
    protected AnimationController.State animationState;
    @Shadow
    protected AnimationController.ParticleKeyframeHandler<T> particleKeyframeHandler;

    @Unique
    private AnimationController.ParticleKeyframeHandler<T> particlestorm$wrappedParticleHandler;

    @Unique
    private List<GeoBone> particlestorm$bonesWhichHasLocators;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void particlestorm$registerReloadCallback(CallbackInfo ci) {
        GeckoLibHelper.addReloadCallback(() -> particlestorm$bonesWhichHasLocators = null);
    }

    @Override
    public List<GeoBone> particlestorm$getBonesWhichHasLocators() {
        return Objects.requireNonNullElse(particlestorm$bonesWhichHasLocators, List.of());
    }

    @Override
    public void particlestorm$setBonesWhichHasLocators(Collection<CoreGeoBone> registeredBones) {
        if (particlestorm$bonesWhichHasLocators == null) {
            this.particlestorm$bonesWhichHasLocators = registeredBones.stream().filter(GeoBone.class::isInstance).map(GeoBone.class::cast).filter(bone -> {
                Map<String, LocatorValue> locators = IPSGeoBone.of(bone).particlestorm$getLocators();
                return locators != null && !locators.isEmpty();
            }).toList();
        }
    }

    @Inject(method = "process", at = @At("HEAD"))
    private void particlestorm$ensureParticleHandler(CallbackInfo ci) {
        particlestorm$wrapParticleHandler();
    }

    @Inject(method = "setParticleKeyframeHandler", at = @At("TAIL"))
    private void particlestorm$wrapCustomParticleHandler(AnimationController.ParticleKeyframeHandler<T> handler, CallbackInfoReturnable<AnimationController<T>> cir) {
        particlestorm$wrapParticleHandler();
    }

    @Unique
    private void particlestorm$wrapParticleHandler() {
        if (particlestorm$wrappedParticleHandler != null && particleKeyframeHandler == particlestorm$wrappedParticleHandler) return;
        AnimationController.ParticleKeyframeHandler<T> original = particleKeyframeHandler;
        particlestorm$wrappedParticleHandler = event -> {
            GeckoLibHelper.processParticleEffect(event.getAnimatable(), event.getController(), event.getKeyframeData());
            if (original != null) original.handle(event);
        };
        particleKeyframeHandler = particlestorm$wrappedParticleHandler;
    }

    @Inject(method = "resetEventKeyFrames", at = @At("HEAD"))
    private void removeEmitters(CallbackInfo ci) {
        GeckoLibHelper.removeEmittersWhenAnimationChange(animationState, animatable.getAnimatableInstanceCache());
    }
}

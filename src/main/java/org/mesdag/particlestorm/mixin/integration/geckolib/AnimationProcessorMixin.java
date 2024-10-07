package org.mesdag.particlestorm.mixin.integration.geckolib;

import org.mesdag.particlestorm.mixinauxi.IGeoBone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.animation.AnimationProcessor", remap = false)
public abstract class AnimationProcessorMixin {
    @Shadow
    public abstract Collection<GeoBone> getRegisteredBones();

    @Unique
    private List<GeoBone> particlestorm$bonesWhichHasLocators;

    @Inject(method = "tickAnimation", at = @At("TAIL"))
    private void tickLocators(GeoAnimatable animatable, GeoModel<GeoAnimatable> model, AnimatableManager<GeoAnimatable> animatableManager, double animTime, AnimationState<GeoAnimatable> state, boolean crashWhenCantFindBone, CallbackInfo ci) {
        if (particlestorm$bonesWhichHasLocators == null) {
            this.particlestorm$bonesWhichHasLocators = getRegisteredBones().stream()
                    .filter(bone -> ((IGeoBone) bone).particlestorm$getLocators() != null)
                    .collect(Collectors.toList());
        }
        for (GeoBone geoBone : particlestorm$bonesWhichHasLocators) {
            IGeoBone iGeoBone = (IGeoBone) geoBone;
            iGeoBone.particlestorm$getLocators(); // todo
        }
    }
}

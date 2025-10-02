package org.mesdag.particlestorm.mixin.integration.geckolib;

import com.llamalad7.mixinextras.sugar.Local;
import org.mesdag.particlestorm.mixed.IAnimationController;
import org.mesdag.particlestorm.mixed.IGeoBone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.animation.AnimationProcessor", remap = false)
public abstract class AnimationProcessorMixin<T extends GeoAnimatable> {
    @Shadow
    public abstract Collection<GeoBone> getRegisteredBones();

    @Unique
    private List<GeoBone> particlestorm$bonesWhichHasLocators;

    @Inject(method = "tickAnimation", at = @At(value = "INVOKE", target = "Lsoftware/bernie/geckolib/animation/AnimationController;process(Lsoftware/bernie/geckolib/model/GeoModel;Lsoftware/bernie/geckolib/animation/AnimationState;Ljava/util/Map;Ljava/util/Map;DZ)V"))
    private void tickLocators(T animatable, GeoModel<T> model, AnimatableManager<T> animatableManager, double animTime, AnimationState<T> state, boolean crashWhenCantFindBone, CallbackInfo ci, @Local AnimationController<T> controller) {
        if (particlestorm$bonesWhichHasLocators == null) {
            this.particlestorm$bonesWhichHasLocators = getRegisteredBones().stream()
                    .filter(bone -> IGeoBone.of(bone).particlestorm$getLocators() != null)
                    .collect(Collectors.toList());
        }
        ((IAnimationController) controller).particlestorm$setBonesWhichHasLocators(particlestorm$bonesWhichHasLocators);
    }
}

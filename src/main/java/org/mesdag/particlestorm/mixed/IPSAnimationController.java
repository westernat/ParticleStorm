package org.mesdag.particlestorm.mixed;

import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;

import java.util.Collection;
import java.util.List;

public interface IPSAnimationController {
    List<GeoBone> particlestorm$getBonesWhichHasLocators();

    void particlestorm$setBonesWhichHasLocators(Collection<CoreGeoBone> registeredBones);

    static IPSAnimationController of(AnimationController<?> controller) {
        return (IPSAnimationController) controller;
    }
}

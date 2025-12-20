package org.mesdag.particlestorm.mixed;

import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.Collection;
import java.util.List;

public interface IAnimationController {
    List<GeoBone> particlestorm$getBonesWhichHasLocators();

    void particlestorm$setBonesWhichHasLocators(Collection<GeoBone> registeredBones);

    static IAnimationController of(AnimationController<?> controller) {
        return (IAnimationController) controller;
    }
}

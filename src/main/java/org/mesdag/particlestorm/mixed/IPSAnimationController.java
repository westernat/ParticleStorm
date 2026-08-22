package org.mesdag.particlestorm.mixed;

import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationController;

import java.util.Collection;
import java.util.List;

public interface IPSAnimationController {
    List<GeoBone> particlestorm$getBonesWhichHasLocators();

    void particlestorm$setBonesWhichHasLocators(Collection<GeoBone> registeredBones);

    static IPSAnimationController of(AnimationController<?> controller) {
        return (IPSAnimationController) controller;
    }
}

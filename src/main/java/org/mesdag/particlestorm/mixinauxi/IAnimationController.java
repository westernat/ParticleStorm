package org.mesdag.particlestorm.mixinauxi;

import software.bernie.geckolib.cache.object.GeoBone;

import java.util.List;

public interface IAnimationController {
    List<GeoBone> particlestorm$getBonesWhichHasLocators();

    void particlestorm$setBonesWhichHasLocators(List<GeoBone> bones);
}

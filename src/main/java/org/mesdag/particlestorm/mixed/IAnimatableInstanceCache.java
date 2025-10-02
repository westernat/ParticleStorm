package org.mesdag.particlestorm.mixed;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

public interface IAnimatableInstanceCache {
    Object2IntMap<LocatorValue> particlestorm$getCachedId();

    Vector3f particlestorm$getPosition();

    Vector3f particlestorm$getRotation();

    Vector3f particlestorm$getScale();

    static IAnimatableInstanceCache of(AnimatableInstanceCache cache) {
        return (IAnimatableInstanceCache) cache;
    }
}

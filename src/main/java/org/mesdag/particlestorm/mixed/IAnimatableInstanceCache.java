package org.mesdag.particlestorm.mixed;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

public interface IAnimatableInstanceCache {
    Object2IntMap<LocatorValue> particlestorm$getCachedId();

    /// \[position, rotation\]
    Vector3f[] particlestorm$getTransform(GeoBone bone);

    static IAnimatableInstanceCache of(AnimatableInstanceCache cache) {
        return (IAnimatableInstanceCache) cache;
    }
}

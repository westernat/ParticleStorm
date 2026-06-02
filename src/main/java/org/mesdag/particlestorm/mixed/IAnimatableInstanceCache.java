package org.mesdag.particlestorm.mixed;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

public interface IAnimatableInstanceCache {
    Object2IntMap<LocatorValue> particlestorm$getCachedId();

    GeckoLibHelper.LocatorState particlestorm$getLocatorState(LocatorValue locator);

    static IAnimatableInstanceCache of(AnimatableInstanceCache cache) {
        return (IAnimatableInstanceCache) cache;
    }
}

package org.mesdag.particlestorm.mixed;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

public interface IPSAnimatableInstanceCache {
    Object2IntMap<LocatorValue> particlestorm$getCachedId();

    GeckoLibHelper.LocatorState particlestorm$getLocatorState(LocatorValue locator);

    static IPSAnimatableInstanceCache of(AnimatableInstanceCache cache) {
        return (IPSAnimatableInstanceCache) cache;
    }
}

package org.mesdag.particlestorm.mixed;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

public interface IPSAnimatableInstanceCache {
    Object2ObjectMap<LocatorValue, IntList> particlestorm$getCachedId();

    void particlestorm$createLocatorState(LocatorValue locator);

    @Nullable GeckoLibHelper.LocatorState particlestorm$getLocatorState(LocatorValue locator);

    static IPSAnimatableInstanceCache of(AnimatableInstanceCache cache) {
        return (IPSAnimatableInstanceCache) cache;
    }
}

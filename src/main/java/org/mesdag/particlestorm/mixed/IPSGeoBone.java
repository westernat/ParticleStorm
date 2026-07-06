package org.mesdag.particlestorm.mixed;

import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.util.Map;

public interface IPSGeoBone {
    @Nullable Map<String, LocatorValue> particlestorm$getLocators();

    void particlestorm$setLocators(Map<String, LocatorValue> locators);

    static IPSGeoBone of(GeoBone geoBone) {
        return (IPSGeoBone) geoBone;
    }
}

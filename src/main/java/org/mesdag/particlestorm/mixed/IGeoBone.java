package org.mesdag.particlestorm.mixed;

import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.util.Map;

public interface IGeoBone {
    Map<String, LocatorValue> particlestorm$getLocators();

    void particlestorm$setLocators(Map<String, LocatorValue> locators);

    static IGeoBone of(GeoBone geoBone) {
        return (IGeoBone) geoBone;
    }
}

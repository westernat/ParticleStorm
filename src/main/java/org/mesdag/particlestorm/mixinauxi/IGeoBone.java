package org.mesdag.particlestorm.mixinauxi;

import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.util.Map;

public interface IGeoBone {
    Map<String, LocatorValue> particlestorm$getLocators();

    void particlestorm$setLocators(Map<String, LocatorValue> locators);
}

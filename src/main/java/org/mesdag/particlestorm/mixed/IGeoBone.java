package org.mesdag.particlestorm.mixed;

import org.joml.Vector3f;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.util.Map;

public interface IGeoBone {
    Map<String, LocatorValue> particlestorm$getLocators();

    void particlestorm$setLocators(Map<String, LocatorValue> locators);

    Vector3f particlestorm$getRotation();
}

package org.mesdag.particlestorm.mixin.integration.geckolib;

import org.mesdag.particlestorm.mixinauxi.IGeoBone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.util.Map;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.cache.object.GeoBone", remap = false)
public abstract class GeoBoneMixin implements IGeoBone {
    @Unique
    private Map<String, LocatorValue> particlestorm$locators;

    @Override
    public Map<String, LocatorValue> particlestorm$getLocators() {
        return particlestorm$locators;
    }

    @Override
    public void particlestorm$setLocators(Map<String, LocatorValue> locators) {
        this.particlestorm$locators = locators;
    }
}

package org.mesdag.particlestorm.mixin.integration.geckolib;

import org.joml.Vector3f;
import org.mesdag.particlestorm.mixinauxi.IGeoBone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.util.Map;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.cache.object.GeoBone", remap = false)
public abstract class GeoBoneMixin implements IGeoBone {
    @Unique
    private Map<String, LocatorValue> particlestorm$locators;
    @Unique
    private final Vector3f particlestorm$rotation = new Vector3f();

    @Override
    public Map<String, LocatorValue> particlestorm$getLocators() {
        return particlestorm$locators;
    }

    @Override
    public void particlestorm$setLocators(Map<String, LocatorValue> locators) {
        this.particlestorm$locators = locators;
    }

    @Override
    public Vector3f particlestorm$getRotation() {
        return particlestorm$rotation;
    }

    @Inject(method = "setRotX", at = @At("TAIL"))
    private void setRotX(float value, CallbackInfo ci) {
        particlestorm$rotation.x = value;
    }

    @Inject(method = "setRotY", at = @At("TAIL"))
    private void setRotY(float value, CallbackInfo ci) {
        particlestorm$rotation.y = value;
    }

    @Inject(method = "setRotZ", at = @At("TAIL"))
    private void setRotZ(float value, CallbackInfo ci) {
        particlestorm$rotation.z = value;
    }
}

package org.mesdag.particlestorm.mixin.integration.geckolib;

import org.joml.Vector3f;
import org.mesdag.particlestorm.mixed.IAnimatableInstanceCache;
import org.mesdag.particlestorm.mixed.IGeoBone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.LocatorValue;
import software.bernie.geckolib.loading.math.MolangQueries;

import java.util.Map;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.cache.object.GeoBone", remap = false)
public abstract class GeoBoneMixin implements IGeoBone {
    @Shadow
    private float positionX;
    @Shadow
    private float positionY;
    @Shadow
    private float positionZ;
    @Shadow
    private float rotX;
    @Shadow
    private float rotY;
    @Shadow
    private float rotZ;
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

    @Inject(method = "resetStateChanges", at = @At("TAIL"))
    private void setData(CallbackInfo ci) {
        if (particlestorm$locators == null || particlestorm$locators.isEmpty()) return;
        MolangQueries.Actor<?> actor = MolangQueriesAccessor.callGetActor();
        if (actor != null && actor.animatable() instanceof GeoAnimatable animatable) {
            IAnimatableInstanceCache cache = IAnimatableInstanceCache.of(animatable.getAnimatableInstanceCache());
            Vector3f[] transform = cache.particlestorm$getTransform((GeoBone) (Object) this);
            transform[0].set(positionX, positionY, positionZ);
            transform[1].set(rotX, rotY, rotZ);
            //cache.particlestorm$getScale().set(scaleX, scaleY, scaleZ); todo
        }
    }
}

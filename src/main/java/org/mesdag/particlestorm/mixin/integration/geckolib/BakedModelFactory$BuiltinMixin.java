package org.mesdag.particlestorm.mixin.integration.geckolib;

import org.mesdag.particlestorm.mixinauxi.IGeoBone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.ModelProperties;
import software.bernie.geckolib.loading.object.BoneStructure;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.loading.object.BakedModelFactory$Builtin", remap = false)
public abstract class BakedModelFactory$BuiltinMixin {
    @Inject(method = "constructBone", at = @At("RETURN"))
    private void addLocators(BoneStructure boneStructure, ModelProperties properties, GeoBone parent, CallbackInfoReturnable<GeoBone> cir) {
        ((IGeoBone) cir.getReturnValue()).particlestorm$setLocators(boneStructure.self().locators());
    }
}

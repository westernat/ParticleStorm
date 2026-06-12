package org.mesdag.particlestorm.mixin.integration.geckolib;

import com.llamalad7.mixinextras.sugar.Local;
import org.mesdag.particlestorm.mixed.IGeoBone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.object.BoneStructure;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.loading.object.BakedModelFactory$Builtin", remap = false)
public abstract class BakedModelFactory$BuiltinMixin {
    @Inject(method = "constructBone", at = @At("RETURN"))
    private void addLocators(CallbackInfoReturnable<GeoBone> cir, @Local(argsOnly = true) BoneStructure boneStructure) {
        IGeoBone.of(cir.getReturnValue()).particlestorm$setLocators(boneStructure.self().locators());
    }
}

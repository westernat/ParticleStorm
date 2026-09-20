package org.mesdag.particlestorm.mixin.integration.geckolib;

import net.minecraft.world.entity.Entity;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoAnimatable;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.renderer.GeoReplacedEntityRenderer", remap = false)
public abstract class GeoReplacedEntityRendererMixin<E extends Entity, T extends GeoAnimatable> {
    @Shadow
    @Final
    protected T animatable;
    @Shadow
    protected E currentEntity;

    @Inject(method = "preRender", at = @At("HEAD"))
    private void setCurrentEntity(CallbackInfo ci) {
        GeckoLibHelper.setCurrentEntity(animatable, currentEntity);
    }

    @Inject(method = "doPostRenderCleanup", at = @At("TAIL"))
    private void cleanup(CallbackInfo ci) {
        GeckoLibHelper.setCurrentEntity(animatable, null);
    }
}

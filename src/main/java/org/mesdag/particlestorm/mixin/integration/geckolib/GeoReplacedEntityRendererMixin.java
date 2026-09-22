package org.mesdag.particlestorm.mixin.integration.geckolib;

import com.geckolib.animatable.GeoAnimatable;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.geckolib.renderer.GeoReplacedEntityRenderer", remap = false)
public abstract class GeoReplacedEntityRendererMixin<T extends GeoAnimatable, E extends Entity, R extends EntityRenderState> {
    @Shadow
    @Final
    protected T animatable;

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void particlestorm$setCurrentEntity(E entity, R renderState, float partialTick, CallbackInfo ci) {
        GeckoLibHelper.setCurrentEntity(animatable, entity);
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void particlestorm$clearCurrentEntity(E entity, R renderState, float partialTick, CallbackInfo ci) {
        GeckoLibHelper.setCurrentEntity(animatable, null);
    }
}

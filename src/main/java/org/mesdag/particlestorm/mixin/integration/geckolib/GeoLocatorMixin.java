package org.mesdag.particlestorm.mixin.integration.geckolib;

import com.geckolib.cache.model.GeoLocator;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.geckolib.cache.model.GeoLocator", remap = false)
public abstract class GeoLocatorMixin {
    @Inject(
            method = "updatePositionListeners",
            at = @At(value = "INVOKE", target = "Lcom/geckolib/util/RenderUtil;providePositionsToListeners(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/geckolib/renderer/base/RenderPassInfo;[Lcom/geckolib/renderer/base/RenderPassInfo$BonePositionListener;)V")
    )
    private void particlestorm$captureLocatorTransform(PoseStack poseStack, RenderPassInfo<?> renderPassInfo, CallbackInfo ci) {
        GeckoLibHelper.captureLocatorTransform((GeoLocator) (Object) this, poseStack, renderPassInfo);
    }
}

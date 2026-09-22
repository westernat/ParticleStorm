package org.mesdag.particlestorm.mixin.integration.geckolib;

import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.geckolib.renderer.base.RenderPassInfo", remap = false)
public abstract class RenderPassInfoMixin {
    @Inject(method = "create", at = @At("RETURN"))
    private static <R extends GeoRenderState> void particlestorm$attachLocatorListeners(GeoRenderer<?, ?, R> renderer, R renderState, PoseStack poseStack, CameraRenderState cameraState, boolean willRender, CallbackInfoReturnable<RenderPassInfo<R>> cir) {
        GeckoLibHelper.attachLocatorListeners(renderState, cir.getReturnValue());
    }

    @Inject(method = "renderPosed", at = @At("HEAD"))
    private void particlestorm$enterRenderPass(Runnable renderTask, CallbackInfo ci) {
        GeckoLibHelper.enterRenderPass((RenderPassInfo<?>) (Object) this);
    }

    @Inject(method = "renderPosed", at = @At("RETURN"))
    private void particlestorm$exitRenderPass(Runnable renderTask, CallbackInfo ci) {
        GeckoLibHelper.exitRenderPass((RenderPassInfo<?>) (Object) this);
    }
}

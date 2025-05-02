package org.mesdag.particlestorm.mixin.integration.geckolib;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
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

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lsoftware/bernie/geckolib/renderer/GeoReplacedEntityRenderer;defaultRender(Lcom/mojang/blaze3d/vertex/PoseStack;Lsoftware/bernie/geckolib/animatable/GeoAnimatable;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFI)V"))
    private void setCurrentEntity(E entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        GeckoLibHelper.setCurrentEntity(animatable, entity);
    }

    @Inject(method = "doPostRenderCleanup", at = @At("TAIL"))
    private void cleanup(CallbackInfo ci) {
        GeckoLibHelper.setCurrentEntity(animatable, null);
    }
}

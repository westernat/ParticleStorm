package org.mesdag.particlestorm.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRenderMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V", ordinal = 0, remap = false, shift = At.Shift.AFTER))
    private void renderTransparency(
            CallbackInfo ci,
            @Local(argsOnly = true) PoseStack poseStack,
            @Local(argsOnly = true) float partialTick,
            @Local(argsOnly = true) LightTexture lightTexture,
            @Local(argsOnly = true) Camera camera,
            @Local @Nullable Frustum frustum
    ) {
        if (frustum == null) return;
        MolangParticleEngine.INSTANCE.renderParticles(lightTexture, minecraft.textureManager, poseStack, camera, partialTick, frustum, type -> true);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", ordinal = 17))
    private void renderSolidParticles(
            CallbackInfo ci,
            @Local(argsOnly = true) PoseStack poseStack,
            @Local(argsOnly = true) float partialTick,
            @Local(argsOnly = true) LightTexture lightTexture,
            @Local(argsOnly = true) Camera camera,
            @Local ProfilerFiller profilerfiller,
            @Local @Nullable Frustum frustum
    ) {
        if (frustum == null) return;
        profilerfiller.popPush("solid_molang_particles");
        RenderSystem.depthMask(true);
        MolangParticleEngine.INSTANCE.renderParticles(lightTexture, minecraft.textureManager, poseStack, camera, partialTick, frustum, PSGameClient::isNotTranslucent);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V", ordinal = 1, remap = false, shift = At.Shift.AFTER))
    private void renderNonTransparency(
            CallbackInfo ci,
            @Local(argsOnly = true) PoseStack poseStack,
            @Local(argsOnly = true) float partialTick,
            @Local(argsOnly = true) LightTexture lightTexture,
            @Local(argsOnly = true) Camera camera,
            @Local @Nullable Frustum frustum
    ) {
        if (frustum == null) return;
        MolangParticleEngine.INSTANCE.renderParticles(lightTexture, minecraft.textureManager, poseStack, camera, partialTick, frustum, PSGameClient::isTranslucent);
    }
}

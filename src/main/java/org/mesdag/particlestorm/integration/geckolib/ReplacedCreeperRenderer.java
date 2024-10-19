package org.mesdag.particlestorm.integration.geckolib;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Creeper;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

public class ReplacedCreeperRenderer extends GeoReplacedEntityRenderer<Creeper, ReplacedCreeperEntity> {
    public ReplacedCreeperRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ReplacedCreeperModel(), new ReplacedCreeperEntity());
    }

    @Override
    public void preRender(PoseStack poseStack, ReplacedCreeperEntity animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        float swellFactor = this.currentEntity.getSwelling(partialTick);
        float swellMod = 1.0F + Mth.sin(swellFactor * 100.0F) * swellFactor * 0.01F;
        swellFactor = (float)Math.pow(Mth.clamp(swellFactor, 0.0F, 1.0F), 3.0);
        float horizontalSwell = (1.0F + swellFactor * 0.4F) * swellMod;
        float verticalSwell = (1.0F + swellFactor * 0.1F) / swellMod;
        poseStack.scale(horizontalSwell, verticalSwell, horizontalSwell);
    }

    public int getPackedOverlay(ReplacedCreeperEntity animatable, float u, float partialTick) {
        return super.getPackedOverlay(animatable, this.getSwellOverlay(this.currentEntity, u), partialTick);
    }

    protected float getSwellOverlay(Creeper entity, float u) {
        float swell = entity.getSwelling(u);
        return (int)(swell * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(swell, 0.5F, 1.0F);
    }
}

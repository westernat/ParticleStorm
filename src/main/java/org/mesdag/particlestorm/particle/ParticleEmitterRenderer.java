package org.mesdag.particlestorm.particle;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class ParticleEmitterRenderer extends EntityRenderer<ParticleEmitterEntity> {
    public ParticleEmitterRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.shadowStrength = 0.0F;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ParticleEmitterEntity entity) {
        return TextureAtlas.LOCATION_PARTICLES;
    }

    @Override
    public boolean shouldRender(@NotNull ParticleEmitterEntity livingEntity, @NotNull Frustum camera, double camX, double camY, double camZ) {
        return false;
    }
}

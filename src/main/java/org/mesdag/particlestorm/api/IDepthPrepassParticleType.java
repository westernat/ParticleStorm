package org.mesdag.particlestorm.api;

import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.particle.ParticleRenderType;

public interface IDepthPrepassParticleType extends ParticleRenderType {
    @Override
    default void end(Tesselator tesselator) {
        tesselator.end();
    }
}

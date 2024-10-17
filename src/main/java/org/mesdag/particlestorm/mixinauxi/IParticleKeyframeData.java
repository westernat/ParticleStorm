package org.mesdag.particlestorm.mixinauxi;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.mesdag.particlestorm.data.molang.MolangExp;

public interface IParticleKeyframeData {
    ResourceLocation particlestorm$getParticle();

    MolangExp particlestorm$getExpression(Entity entity);

    int[] particlestorm$getCachedId(int size);
}

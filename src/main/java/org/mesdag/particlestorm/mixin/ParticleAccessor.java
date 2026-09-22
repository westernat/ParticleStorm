package org.mesdag.particlestorm.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor("x")
    double particlestorm$getX();

    @Accessor("y")
    double particlestorm$getY();

    @Accessor("z")
    double particlestorm$getZ();
}

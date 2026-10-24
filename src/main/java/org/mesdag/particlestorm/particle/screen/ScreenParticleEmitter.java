package org.mesdag.particlestorm.particle.screen;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.mesdag.particlestorm.data.event.ParticleEffect;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.ParticleEmitter;

public class ScreenParticleEmitter extends ParticleEmitter {
    public ScreenParticleEmitter(ResourceLocation type, Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression) {
        super(type, level, pos, particleId, expression);
    }

    public ScreenParticleEmitter(ParticleEmitter parent, ParticleEffect effect) {
        super(parent, effect);
    }
}

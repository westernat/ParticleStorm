package org.mesdag.particlestorm.api;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import org.mesdag.particlestorm.particle.MolangParticleOption;

public class MolangParticleMobEffect extends MobEffect {
    public MolangParticleMobEffect(MobEffectCategory category, int color, Identifier particleId) {
        super(category, color, new MolangParticleOption(particleId));
    }
}

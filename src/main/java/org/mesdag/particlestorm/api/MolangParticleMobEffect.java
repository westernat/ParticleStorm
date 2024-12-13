package org.mesdag.particlestorm.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import org.mesdag.particlestorm.particle.MolangParticleOption;

/**
 * "minecraft:emitter_rate_manual" is required
 * @see org.mesdag.particlestorm.data.component.EmitterRate.Type
 */
public class MolangParticleMobEffect extends MobEffect {
    public MolangParticleMobEffect(MobEffectCategory category, int color, ResourceLocation particleId) {
        super(category, color, new MolangParticleOption(particleId));
    }
}

package org.mesdag.particlestorm.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class MolangParticleMobEffect extends MobEffect {
    private final ResourceLocation particleId;

    public MolangParticleMobEffect(MobEffectCategory category, int color, ResourceLocation particleId) {
        super(category, color);
        this.particleId = particleId;
    }

    public ResourceLocation getParticleId() {
        return particleId;
    }
}

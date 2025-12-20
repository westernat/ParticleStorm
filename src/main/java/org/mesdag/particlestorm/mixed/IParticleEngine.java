package org.mesdag.particlestorm.mixed;

import net.minecraft.client.particle.ParticleEngine;

public interface IParticleEngine {
    void particlestorm$bindSprites();

    static IParticleEngine of(ParticleEngine engine) {
        return (IParticleEngine) engine;
    }
}

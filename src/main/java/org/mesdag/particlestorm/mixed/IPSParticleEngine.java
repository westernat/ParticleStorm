package org.mesdag.particlestorm.mixed;

import net.minecraft.client.particle.ParticleEngine;

public interface IPSParticleEngine {
    void particlestorm$bindSprites();

    static IPSParticleEngine of(ParticleEngine engine) {
        return (IPSParticleEngine) engine;
    }
}

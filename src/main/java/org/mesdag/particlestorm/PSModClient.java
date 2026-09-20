package org.mesdag.particlestorm;

import org.mesdag.particlestorm.api.RegisterCustomParticleTypeEvent;
import org.mesdag.particlestorm.api.RegisterCustomEmitterTypeEvent;
import org.mesdag.particlestorm.api.RegisterCustomComponentEvent;
import org.mesdag.particlestorm.api.RegisterCustomEventNodeEvent;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

public final class PSModClient {
    private PSModClient() {
    }

    public static void registerCustomParticleType(RegisterCustomParticleTypeEvent event) {
        event.registerWithSprites(ParticleStorm.MOLANG, (emitter, particlePreset, level, x, y, z, sprites) ->
                new MolangParticleInstance(particlePreset, level, x, y, z, level.getRandom())
        );
    }

    public static void registerCustomEmitterType(RegisterCustomEmitterTypeEvent event) {
    }

    public static void registerCustomComponent(RegisterCustomComponentEvent event) {
    }

    public static void registerCustomEventNode(RegisterCustomEventNodeEvent event) {
    }
}

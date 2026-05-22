package org.mesdag.particlestorm.api;

import org.mesdag.particlestorm.particle.ParticleEmitter;

public interface IEmitterComponent extends IComponent {
    default void update(ParticleEmitter emitter) {}

    default void apply(ParticleEmitter emitter) {}

    default boolean requireUpdate() {
        return false;
    }
}

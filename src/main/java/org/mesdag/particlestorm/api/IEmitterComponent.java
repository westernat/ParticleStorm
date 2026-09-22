package org.mesdag.particlestorm.api;

import org.mesdag.particlestorm.particle.ParticleEmitter;

public interface IEmitterComponent extends IComponent {
    default void update(ParticleEmitter entity) {}

    default void apply(ParticleEmitter entity) {}

    default boolean requireUpdate() {
        return false;
    }
}

package org.mesdag.particlestorm.data.component;

import org.mesdag.particlestorm.particle.ParticleEmitterEntity;

public interface IEmitterComponent extends IComponent {
    default void update(ParticleEmitterEntity entity) {}

    default void apply(ParticleEmitterEntity entity) {}

    default boolean requireUpdate() {
        return false;
    }
}

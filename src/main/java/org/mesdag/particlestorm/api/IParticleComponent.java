package org.mesdag.particlestorm.api;

import net.minecraft.world.level.Level;

public interface IParticleComponent extends IComponent {
    default void update(IMolangParticleInstance instance) {}

    default void apply(IMolangParticleInstance instance) {}

    default boolean requireUpdate() {
        return false;
    }

    default void initialize(Level level) {}
}

package org.mesdag.particlestorm.api;

import net.minecraft.world.level.Level;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

public interface IParticleComponent extends IComponent {
    default void update(MolangParticleInstance instance) {}

    default void apply(MolangParticleInstance instance) {}

    default boolean requireUpdate() {
        return false;
    }

    default void initialize(Level level) {}
}

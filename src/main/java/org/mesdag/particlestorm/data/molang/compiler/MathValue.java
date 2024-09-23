package org.mesdag.particlestorm.data.molang.compiler;

import org.mesdag.particlestorm.particle.MolangParticleInstance;

import java.util.function.ToDoubleFunction;

/**
 * Base interface for all computational values in the math system
 * <p>
 * All mathematical objects are an extension of this interface, allowing for an indefinitely-nestable
 * mathematical system that can be accessed via this one access point
 */
public interface MathValue extends ToDoubleFunction<MolangParticleInstance> {
    /**
     * Get computed or stored value
     */
    double get(MolangParticleInstance instance);

    /**
     * Return whether this type of MathValue should be considered mutable; its value could change.
     * <br>
     * This is used to cache calculated values, optimising computational work
     */
    default boolean isMutable() {
        return true;
    }

    /**
     * Use {@link #get}
     */
    @Deprecated()
    @Override
    default double applyAsDouble(MolangParticleInstance instance) {
        return get(instance);
    }
}

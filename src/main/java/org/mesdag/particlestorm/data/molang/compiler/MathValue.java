package org.mesdag.particlestorm.data.molang.compiler;

import org.mesdag.particlestorm.data.molang.MolangInstance;

import java.util.function.ToDoubleFunction;

/**
 * Base interface for all computational values in the math system
 * <p>
 * All mathematical objects are an extension of this interface, allowing for an indefinitely-nestable
 * mathematical system that can be accessed via this one access point
 */
public interface MathValue extends ToDoubleFunction<MolangInstance> {
    /**
     * Get computed or stored value
     */
    double get(MolangInstance instance);

    default void set(ToDoubleFunction<MolangInstance> function) {}

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
    default double applyAsDouble(MolangInstance instance) {
        return get(instance);
    }
}

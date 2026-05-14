package org.mesdag.particlestorm.data.molang.compiler;

import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.api.ToFloatFunction;

/// Base interface for all computational values in the math system
///
/// All mathematical objects are an extension of this interface, allowing for an indefinitely-nestable
/// mathematical system that can be accessed via this one access point
public interface MathValue extends ToFloatFunction<MolangInstance> {
    /// Get computed or stored value
    float get(MolangInstance instance);

    default void set(ToFloatFunction<MolangInstance> function) {}

    /// Return whether this type of MathValue should be considered mutable; its value could change.
    ///
    /// This is used to cache calculated values, optimising computational work
    default boolean isMutable() {
        return true;
    }

    void markImmutable();

    /// Use [#get]
    @Deprecated
    @Override
    default float applyAsFloat(MolangInstance instance) {
        return get(instance);
    }
}

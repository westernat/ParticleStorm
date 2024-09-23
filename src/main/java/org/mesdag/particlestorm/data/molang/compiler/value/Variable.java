package org.mesdag.particlestorm.data.molang.compiler.value;

import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.ToDoubleFunction;

/**
 * {@link MathValue} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Returns the currently stored value, which may be modified at any given time via {@link #set}. Values may be lazily evaluated to eliminate wasteful usage
 */
public record Variable(String name, AtomicReference<ToDoubleFunction<MolangParticleInstance>> value) implements MathValue {
    public Variable(String name, double value) {
        this(name, instance -> value);
    }

    public Variable(String name, ToDoubleFunction<MolangParticleInstance> value) {
        this(name, new AtomicReference<>(value));
    }

    @Override
    public double get(MolangParticleInstance instance) {
        try {
            return this.value.get().applyAsDouble(instance);
        } catch (Exception ex) {
            ParticleStorm.LOGGER.error("Attempted to use Molang variable for incompatible animatable type ({}). An animation json needs to be fixed", this.name);
            return 0;
        }
    }

    public void set(final double value) {
        this.value.set(instance -> value);
    }

    public void set(final ToDoubleFunction<MolangParticleInstance> value) {
        this.value.set(value);
    }

    @Override
    public String toString() {
        return this.name + "(" + this.value.get() + ")";
    }
}

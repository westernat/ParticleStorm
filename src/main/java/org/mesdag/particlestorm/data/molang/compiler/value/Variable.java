package org.mesdag.particlestorm.data.molang.compiler.value;

import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;

import java.util.function.ToDoubleFunction;

/**
 * {@link MathValue} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Returns the currently stored value, which may be modified at any given time via {@link #set}. Values may be lazily evaluated to eliminate wasteful usage
 */
public final class Variable implements MathValue {
    private final String name;
    private ToDoubleFunction<MolangInstance> value;
    private Double constant;

    public Variable(String name, ToDoubleFunction<MolangInstance> value) {
        this.name = name;
        this.value = value;
    }

    public Variable(String name, double value) {
        this.name = name;
        this.constant = value;
    }

    @Override
    public double get(MolangInstance instance) {
        try {
            if (constant != null) return constant;
            return value.applyAsDouble(instance);
        } catch (Exception ex) {
            ParticleStorm.LOGGER.error("Attempted to use Molang variable for incompatible animatable type ({}). An animation json needs to be fixed", this.name);
            return 0;
        }
    }

    public void set(Double value) {
        this.constant = value;
    }

    @Override
    public void set(ToDoubleFunction<MolangInstance> value) {
        this.value = value;
    }

    public String name() {
        return name;
    }

    public ToDoubleFunction<MolangInstance> value() {
        return value;
    }
}

package org.mesdag.particlestorm.data.molang.compiler.value;

import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

/**
 * {@link MathValue} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Returns one of two stored values dependent on the result of the stored condition value.
 * This returns such that a non-zero result from the condition will return the <b>true</b> stored value, otherwise returning the <b>false</b> stored value
 */
public record Ternary(MathValue condition, MathValue trueValue, MathValue falseValue) implements MathValue {
    @Override
    public double get(MolangParticleInstance instance) {
        return this.condition.get(instance) != 0 ? this.trueValue.get(instance) : this.falseValue.get(instance);
    }

    @Override
    public boolean isMutable() {
        return this.condition.isMutable() || this.trueValue.isMutable() || this.falseValue.isMutable();
    }

    @Override
    public String toString() {
        return this.condition.toString() + " ? " + this.trueValue.toString() + " : " + this.falseValue.toString();
    }
}

package org.mesdag.particlestorm.data.molang.compiler.value;

import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;

/// [MathValue] value supplier
///
/// **Contract:**
///
/// Returns one of two stored values dependent on the result of the stored condition value.
/// This returns such that a non-zero result from the condition will return the **true** stored value, otherwise returning the **false** stored value
public record Ternary(MathValue condition, MathValue trueValue, MathValue falseValue) implements MathValue {
    @Override
    public float get(MolangInstance instance) {
        return this.condition.get(instance) == 0 ? this.falseValue.get(instance) : this.trueValue.get(instance);
    }

    @Override
    public void markImmutable() {
        condition.markImmutable();
        trueValue.markImmutable();
        falseValue.markImmutable();
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

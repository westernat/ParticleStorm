package org.mesdag.particlestorm.data.molang.compiler.value;

import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;

/// [MathValue] value supplier
///
/// **Contract:**
///
/// Negated equivalent of the stored value; returning a positive number if the stored value is negative, or a negative value if the stored value is positive
public record Negative(MathValue value) implements MathValue {
    @Override
    public float get(MolangInstance instance) {
        return -this.value.get(instance);
    }

    @Override
    public void markImmutable() {
        value.markImmutable();
    }

    @Override
    public boolean isMutable() {
        return this.value.isMutable();
    }

    @Override
    public String toString() {
        if (this.value instanceof Constant)
            return "-" + this.value;

        return "-" + "(" + this.value + ")";
    }
}

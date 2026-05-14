package org.mesdag.particlestorm.data.molang.compiler.value;

import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;

/// [MathValue] value supplier
///
/// **Contract:**
///
/// An immutable double value
public record Constant(float value) implements MathValue {
    @Override
    public float get(MolangInstance instance) {
        return this.value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public void markImmutable() {}

    @Override
    public String toString() {
        return String.valueOf(this.value);
    }
}

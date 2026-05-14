package org.mesdag.particlestorm.data.molang.compiler.value;

import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;

/// [MathValue] value supplier
///
/// **Contract:**
///
/// Returns **1** if the contained value is equal to **0**, otherwise returns **0**
public record BooleanNegate(MathValue value) implements MathValue {
    @Override
    public float get(MolangInstance instance) {
        return this.value.get(instance) == 0 ? 1 : 0;
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
        return "!" + this.value.toString();
    }
}

package org.mesdag.particlestorm.data.molang.compiler.value;

import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;

/// [MathValue] value supplier
///
/// **Contract:**
///
/// Assigns a variable to the given value, then returns 0
public record VariableAssignment(Variable variable, MathValue value) implements MathValue {
    @Override
    public float get(MolangInstance instance) {
        variable.set(value.get(instance));
        return 0;
    }

    @Override
    public void markImmutable() {
        variable.markImmutable();
        value.markImmutable();
    }

    @Override
    public String toString() {
        return variable.name() + "=" + value.toString();
    }
}

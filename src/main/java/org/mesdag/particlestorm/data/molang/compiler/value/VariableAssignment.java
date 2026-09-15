package org.mesdag.particlestorm.data.molang.compiler.value;

import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;

/// [MathValue] value supplier
///
/// **Contract:**
///
/// Assigns a variable to the given value, then returns 0
public record VariableAssignment(String name, MathValue value) implements MathValue {
    @Override
    public float get(MolangInstance instance) {
        Variable localVar = instance.getVars().getVariable(name);
        if (localVar == null) {
            localVar = instance.getVars().setValue(name, value);
        }
        localVar.set(value.get(instance));
        return 0;
    }

    @Override
    public void markImmutable() {
        value.markImmutable();
    }

    @Override
    public String toString() {
        return name + "=" + value.toString();
    }
}

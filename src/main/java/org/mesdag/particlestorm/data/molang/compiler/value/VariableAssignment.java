package org.mesdag.particlestorm.data.molang.compiler.value;

import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;

/**
 * {@link MathValue} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Assigns a variable to the given value, then returns 0
 */
public record VariableAssignment(String name, MathValue value) implements MathValue {
    @Override
    public double get(MolangInstance instance) {
        double calculated = value.get(instance);
        Variable variable = instance.getVars().getVariable(name);
        if (variable == null) {
            instance.getVars().setValue(name, calculated);
        } else {
            variable.set(calculated);
        }
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

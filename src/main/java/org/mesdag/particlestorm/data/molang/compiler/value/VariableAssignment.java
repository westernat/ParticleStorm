package org.mesdag.particlestorm.data.molang.compiler.value;

import org.mesdag.particlestorm.data.molang.MolangData;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;

/**
 * {@link MathValue} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Assigns a variable to the given value, then returns 0
 */
public record VariableAssignment(Variable variable, MathValue value) implements MathValue {
    @Override
    public double get(MolangData instance) {
        variable.set(value.get(instance));
        return 0;
    }

    @Override
    public String toString() {
        return variable.name() + "=" + value.toString();
    }
}

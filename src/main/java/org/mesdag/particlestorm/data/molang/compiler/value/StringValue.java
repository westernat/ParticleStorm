package org.mesdag.particlestorm.data.molang.compiler.value;

import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;

public record StringValue(String value) implements MathValue {
    @Override
    public double get(MolangInstance instance) {
        return value.isEmpty() ? 0.0 : 1.0;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public String toString() {
        return value;
    }
}

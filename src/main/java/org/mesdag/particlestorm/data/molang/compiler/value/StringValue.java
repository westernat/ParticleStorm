package org.mesdag.particlestorm.data.molang.compiler.value;

import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;

public record StringValue(String value) implements MathValue {
    @Override
    public float get(MolangInstance instance) {
        return value.isEmpty() ? 0 : 1;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public void markImmutable() {}

    @Override
    public String toString() {
        return value;
    }
}

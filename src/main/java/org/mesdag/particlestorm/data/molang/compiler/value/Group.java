package org.mesdag.particlestorm.data.molang.compiler.value;

import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;

/// [MathValue] value supplier
///
/// **Contract:**
///
/// An unaltered return of the stored MathValue
public record Group(MathValue contents) implements MathValue {
    @Override
    public float get(MolangInstance instance) {
        return this.contents.get(instance);
    }

    @Override
    public void markImmutable() {
        contents.markImmutable();
    }

    @Override
    public boolean isMutable() {
        return this.contents.isMutable();
    }

    @Override
    public String toString() {
        return "(" + this.contents.toString() + ")";
    }
}

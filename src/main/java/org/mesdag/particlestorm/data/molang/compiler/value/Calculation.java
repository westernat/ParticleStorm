package org.mesdag.particlestorm.data.molang.compiler.value;

import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.Operator;

/// [MathValue] value supplier
///
/// **Contract:**
///
/// A computed value of argA and argB defined by the contract of the [Operator]
public record Calculation(Operator operator, MathValue argA, MathValue argB) implements MathValue {
    @Override
    public float get(MolangInstance instance) {
        return this.operator.compute(this.argA.get(instance), this.argB.get(instance));
    }

    @Override
    public boolean isMutable() {
        return argA.isMutable() || argB.isMutable();
    }

    @Override
    public void markImmutable() {
        argA.markImmutable();
        argB.markImmutable();
    }

    @Override
    public String toString() {
        return this.argA.toString() + " " + this.operator.symbol() + " " + this.argB.toString();
    }
}

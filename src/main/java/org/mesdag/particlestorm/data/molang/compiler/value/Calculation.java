package org.mesdag.particlestorm.data.molang.compiler.value;

import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.Operator;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

/**
 * {@link MathValue} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * A computed value of argA and argB defined by the contract of the {@link Operator}
 */
public final class Calculation implements MathValue {
    private final Operator operator;
    private final MathValue argA;
    private final MathValue argB;
    private final boolean isMutable;

    private double cachedValue = Double.MIN_VALUE;

    public Calculation(Operator operator, MathValue argA, MathValue argB) {
        this.operator = operator;
        this.argA = argA;
        this.argB = argB;
        this.isMutable = this.argA.isMutable() || this.argB.isMutable();
    }

    public Operator operator() {
        return this.operator;
    }

    public MathValue argA() {
        return this.argA;
    }

    public MathValue argB() {
        return this.argB;
    }

    @Override
    public double get(MolangParticleInstance instance) {
        if (this.isMutable)
            return this.operator.compute(this.argA.get(instance), this.argB.get(instance));

        if (this.cachedValue == Double.MIN_VALUE)
            this.cachedValue = this.operator.compute(this.argA.get(instance), this.argB.get(instance));

        return this.cachedValue;
    }

    @Override
    public boolean isMutable() {
        return this.isMutable;
    }

    @Override
    public String toString() {
        return this.argA.toString() + " " + this.operator.symbol() + " " + this.argB.toString();
    }
}
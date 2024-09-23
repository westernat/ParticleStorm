package org.mesdag.particlestorm.data.molang.compiler.function.generic;

import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.function.MathFunction;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

/**
 * {@link MathFunction} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Returns the input value raised to the power of the second input value
 */
public final class PowFunction extends MathFunction {
    private final MathValue value;
    private final MathValue power;

    public PowFunction(MathValue... values) {
        super(values);

        this.value = values[0];
        this.power = values[1];
    }

    @Override
    public String getName() {
        return "math.pow";
    }

    @Override
    public double compute(MolangParticleInstance instance) {
        return Math.pow(this.value.get(instance), this.power.get(instance));
    }

    @Override
    public int getMinArgs() {
        return 2;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[] {this.value, this.power};
    }
}

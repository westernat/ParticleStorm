package org.mesdag.particlestorm.data.molang.compiler.function.round;

import org.mesdag.particlestorm.data.molang.MolangData;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.function.MathFunction;

/**
 * {@link MathFunction} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Returns the closest integer value to the input value
 */
public final class RoundFunction extends MathFunction {
    private final MathValue value;

    public RoundFunction(MathValue... values) {
        super(values);

        this.value = values[0];
    }

    @Override
    public String getName() {
        return "math.round";
    }

    @Override
    public double compute(MolangData instance) {
        return Math.round(this.value.get(instance));
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[] {this.value};
    }
}

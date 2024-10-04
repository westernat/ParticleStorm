package org.mesdag.particlestorm.data.molang.compiler.function.round;

import org.mesdag.particlestorm.data.molang.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.function.MathFunction;

/**
 * {@link MathFunction} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Returns the smallest value that is greater than or equal to the input value and is equal to an integer
 */
public final class CeilFunction extends MathFunction {
    private final MathValue value;

    public CeilFunction(MathValue... values) {
        super(values);

        this.value = values[0];
    }

    @Override
    public String getName() {
        return "math.ceil";
    }

    @Override
    public double compute(MolangInstance instance) {
        return Math.ceil(this.value.get(instance));
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

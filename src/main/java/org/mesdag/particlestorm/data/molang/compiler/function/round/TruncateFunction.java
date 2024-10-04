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
 * Returns the closest value that is equal to the input value or closer to zero, and is equal to an integer
 */
public final class TruncateFunction extends MathFunction {
    private final MathValue value;

    public TruncateFunction(MathValue... values) {
        super(values);

        this.value = values[0];
    }

    @Override
    public String getName() {
        return "math.trunc";
    }

    @Override
    public double compute(MolangInstance instance) {
        return (long)this.value.get(instance);
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

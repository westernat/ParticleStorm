package org.mesdag.particlestorm.data.molang.compiler.function.generic;

import net.minecraft.util.Mth;
import org.mesdag.particlestorm.data.molang.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.function.MathFunction;

/**
 * {@link MathFunction} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Returns the sine of the input value angle, with the input angle converted to radians
 */
public final class SinFunction extends MathFunction {
    private final MathValue value;

    public SinFunction(MathValue... values) {
        super(values);

        this.value = values[0];
    }

    @Override
    public String getName() {
        return "math.sin";
    }

    @Override
    public double compute(MolangInstance instance) {
        return Math.sin(this.value.get(instance) * Mth.DEG_TO_RAD);
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

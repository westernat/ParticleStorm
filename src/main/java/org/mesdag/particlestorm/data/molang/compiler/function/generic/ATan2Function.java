package org.mesdag.particlestorm.data.molang.compiler.function.generic;

import net.minecraft.util.Mth;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.function.MathFunction;

/**
 * {@link MathFunction} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Returns the arc-tangent theta of the input rectangular coordinate values (y,x), with the output converted to degrees
 */
public final class ATan2Function extends MathFunction {
    private final MathValue y;
    private final MathValue x;

    public ATan2Function(MathValue... values) {
        super(values);

        this.y = values[0];
        this.x = values[1];
    }

    @Override
    public String getName() {
        return "math.atan2";
    }

    @Override
    public double compute(MolangInstance instance) {
        return Math.atan2(this.y.get(instance), this.x.get(instance)) * Mth.RAD_TO_DEG;
    }

    @Override
    public int getMinArgs() {
        return 2;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[] {this.y, this.x};
    }
}

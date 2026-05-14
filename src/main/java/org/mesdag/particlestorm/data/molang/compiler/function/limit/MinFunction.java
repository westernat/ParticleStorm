package org.mesdag.particlestorm.data.molang.compiler.function.limit;

import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.function.MathFunction;

/// [MathFunction] value supplier
///
/// **Contract:**
///
/// Returns the lesser of the two input values
public final class MinFunction extends MathFunction {
    private final MathValue valueA;
    private final MathValue valueB;

    public MinFunction(MathValue... values) {
        super(values);

        this.valueA = values[0];
        this.valueB = values[1];
    }

    @Override
    public String getName() {
        return "math.min";
    }

    @Override
    public float compute(MolangInstance instance) {
        return Math.min(valueA.get(instance), valueB.get(instance));
    }

    @Override
    public int getMinArgs() {
        return 2;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[] {valueA, valueB};
    }
}

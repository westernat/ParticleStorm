package org.mesdag.particlestorm.data.molang.compiler.function.generic;

import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.function.MathFunction;

/// [MathFunction] value supplier
///
/// **Contract:**
///
/// Returns the square root of the input value
public final class SqrtFunction extends MathFunction {
    private final MathValue value;

    public SqrtFunction(MathValue... values) {
        super(values);

        this.value = values[0];
    }

    @Override
    public String getName() {
        return "math.sqrt";
    }

    @Override
    public float compute(MolangInstance instance) {
        return (float) Math.sqrt(value.get(instance));
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[] {value};
    }
}

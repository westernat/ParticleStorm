package org.mesdag.particlestorm.data.molang.compiler.function.misc;

import net.minecraft.util.Mth;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.function.MathFunction;
import org.mesdag.particlestorm.data.molang.compiler.value.Constant;

/// [MathFunction] value supplier
///
/// **Contract:**
///
/// Returns <a href="https://en.wikipedia.org/wiki/Pi">PI</a>
public final class PiFunction extends MathFunction {
    public PiFunction(MathValue... values) {
        super(values);
    }

    @Override
    public String getName() {
        return "math.pi";
    }

    @Override
    public float compute(MolangInstance instance) {
        return Mth.PI;
    }

    @Override
    public boolean isMutable(MathValue... values) {
        return false;
    }

    @Override
    public int getMinArgs() {
        return 0;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[] {new Constant(Mth.PI)};
    }
}

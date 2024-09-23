package org.mesdag.particlestorm.data.molang.compiler.function.generic;

import net.minecraft.util.Mth;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.function.MathFunction;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

/**
 * {@link MathFunction} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Returns the cosine of the input value angle, with the input angle converted to radians
 */
public final class CosFunction extends MathFunction {
    private final MathValue value;

    public CosFunction(MathValue... values) {
        super(values);

        this.value = values[0];
    }

    @Override
    public String getName() {
        return "math.cos";
    }

    @Override
    public double compute(MolangParticleInstance instance) {
        return Mth.cos((float)this.value.get(instance) * Mth.DEG_TO_RAD);
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
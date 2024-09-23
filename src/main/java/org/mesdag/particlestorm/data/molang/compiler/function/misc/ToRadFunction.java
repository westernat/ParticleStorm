package org.mesdag.particlestorm.data.molang.compiler.function.misc;

import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.function.MathFunction;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

/**
 * {@link MathFunction} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Converts the input value to radians
 */
public final class ToRadFunction extends MathFunction {
    private final MathValue value;

    public ToRadFunction(MathValue... values) {
        super(values);

        this.value = values[0];
    }

    @Override
    public String getName() {
        return "math.to_rad";
    }

    @Override
    public double compute(MolangParticleInstance instance) {
        return Math.toRadians(this.value.get(instance));
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
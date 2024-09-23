package org.mesdag.particlestorm.data.molang.compiler.function.generic;

import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.function.MathFunction;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

/**
 * {@link MathFunction} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Returns the remainder value of the input value when modulo'd by the modulus value
 */
public final class ModFunction extends MathFunction {
    private final MathValue value;
    private final MathValue modulus;

    public ModFunction(MathValue... values) {
        super(values);

        this.value = values[0];
        this.modulus = values[1];
    }

    @Override
    public String getName() {
        return "math.mod";
    }

    @Override
    public double compute(MolangParticleInstance instance) {
        return this.value.get(instance) % this.modulus.get(instance);
    }

    @Override
    public int getMinArgs() {
        return 2;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[] {this.value, this.modulus};
    }
}

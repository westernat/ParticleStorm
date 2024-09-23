package org.mesdag.particlestorm.data.molang.compiler.value;

import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

/**
 * {@link MathValue} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * An immutable double value
 */
public record Constant(double value) implements MathValue {
    @Override
    public double get(MolangParticleInstance instance) {
        return this.value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public String toString() {
        return String.valueOf(this.value);
    }
}

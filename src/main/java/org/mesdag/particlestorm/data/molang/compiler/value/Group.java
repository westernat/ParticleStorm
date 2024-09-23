package org.mesdag.particlestorm.data.molang.compiler.value;

import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

/**
 * {@link MathValue} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * An unaltered return of the stored MathValue
 */
public record Group(MathValue contents) implements MathValue {
    @Override
    public double get(MolangParticleInstance instance) {
        return this.contents.get(instance);
    }

    @Override
    public boolean isMutable() {
        return this.contents.isMutable();
    }

    @Override
    public String toString() {
        return "(" + this.contents.toString() + ")";
    }
}

package org.mesdag.particlestorm.data.molang;

import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.ToDoubleFunction;

public record ParticleVariable(MathValue raw, AtomicReference<ToDoubleFunction<MolangParticleInstance>> value) implements MathValue {
    public ParticleVariable(MathValue raw, double value) {
        this(raw, new AtomicReference<>(instance -> value));
    }

    public double get(MolangParticleInstance instance) {
        try {
            return value.get().applyAsDouble(instance);
        } catch (Exception ex) {
            ParticleStorm.LOGGER.error("Attempted to use Molang variable for incompatible particle instance. An animation json needs to be fixed");
            return 0;
        }
    }

    public void set(final double constant) {
        value.set(instance -> constant);
    }

    public void set(final ToDoubleFunction<MolangParticleInstance> function) {
        value.set(function);
    }

    @Override
    public String toString() {
        return "(" + raw.get() + ")";
    }

    @Override
    public double get() {
        return raw.get();
    }
}

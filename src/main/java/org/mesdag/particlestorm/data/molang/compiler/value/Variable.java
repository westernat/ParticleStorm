package org.mesdag.particlestorm.data.molang.compiler.value;

import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.api.ToFloatFunction;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;

/// [MathValue] value supplier
///
/// **Contract:**
///
/// Returns the currently stored value, which may be modified at any given time via [#set]. Values may be lazily evaluated to eliminate wasteful usage
public final class Variable implements MathValue {
    private final String name;
    private ToFloatFunction<MolangInstance> value;
    private Float constant;
    private boolean immutable;

    public Variable(String name, ToFloatFunction<MolangInstance> value) {
        this.name = name;
        this.value = value;
    }

    public Variable(String name, float value) {
        this.name = name;
        this.constant = value;
    }

    @Override
    public float get(MolangInstance instance) {
        try {
            float v = constant == null ? value.applyAsFloat(instance) : constant;
            if (immutable) {
                instance.getVars().setValue(name, new Variable(name, v));
            }
            return v;
        } catch (Exception ex) {
            ParticleStorm.LOGGER.error("Attempted to use Molang variable for incompatible animatable type ({}). An animation json needs to be fixed", this.name);
            return 0;
        }
    }

    @Override
    public boolean isMutable() {
        return !immutable;
    }

    @Override
    public void markImmutable() {
        this.immutable = true;
    }

    public void set(Float value) {
        this.constant = value;
    }

    @Override
    public void set(ToFloatFunction<MolangInstance> value) {
        this.value = value;
    }

    public String name() {
        return name;
    }

    public ToFloatFunction<MolangInstance> value() {
        return value;
    }

    public Variable copy() {
        Variable variable;
        if (constant == null) {
            variable = new Variable(name, value);
        } else {
            variable = new Variable(name, constant);
        }
        if (immutable) {
            variable.markImmutable();
        }
        return variable;
    }
}

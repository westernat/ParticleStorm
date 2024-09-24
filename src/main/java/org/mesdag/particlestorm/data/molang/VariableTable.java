package org.mesdag.particlestorm.data.molang;

import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

import java.util.Hashtable;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public class VariableTable {
    public final Hashtable<String, Variable> table;
    public final @Nullable VariableTable previous;

    public VariableTable(Hashtable<String, Variable> table, @Nullable VariableTable previous) {
        this.table = table;
        this.previous = previous;
    }

    public VariableTable(@Nullable VariableTable previous) {
        this(new Hashtable<>(), previous);
    }

    public double getValue(String name, MolangParticleInstance instance) {
        Variable variable = table.get(name);
        if (variable == null) {
            if (previous == null) return 0.0;
            return previous.getValue(name, instance);
        }
        return variable.get(instance);
    }

    public void setValue(String name, ToDoubleFunction<MolangParticleInstance> function) {
        Variable variable = table.get(name);
        if (variable == null) {
            table.put(name, new Variable(name, function));
        } else {
            variable.set(function);
        }
    }

    public void setValue(String name, double value) {
        Variable variable = table.get(name);
        if (variable == null) {
            table.put(name, new Variable(name, value));
        } else {
            variable.set(value);
        }
    }

    public void addVariable(String name, Function<String, Variable> function) {
        table.put(name, function.apply(name));
    }
}

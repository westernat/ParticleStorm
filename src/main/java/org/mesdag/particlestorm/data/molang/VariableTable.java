package org.mesdag.particlestorm.data.molang;

import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

import java.util.Hashtable;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public class VariableTable {
    private final Hashtable<String, Variable> table;

    public VariableTable(Hashtable<String, Variable> table) {
        this.table = table;
    }

    public double getValue(String variable, MolangParticleInstance instance) {
        return table.get(variable).get(instance);
    }

    public void setValue(String variable, ToDoubleFunction<MolangParticleInstance> function) {
        table.get(variable).set(function);
    }

    public void setValue(String variable, double value) {
        table.get(variable).set(instance -> value);
    }

    public void addVariable(String name, Function<String, Variable> function) {
        table.put(name, function.apply(name));
    }
}

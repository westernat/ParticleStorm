package org.mesdag.particlestorm.data.molang;

import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;

import java.util.Hashtable;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public class VariableTable {
    public final Map<String, Variable> table;
    protected VariableTable parent;

    public VariableTable(Map<String, Variable> table, @Nullable VariableTable parent) {
        this.table = table;
        this.parent = parent;
    }

    public VariableTable(@Nullable VariableTable parent) {
        this(new Hashtable<>(), parent);
    }

    public double getValue(String name, MolangInstance instance) {
        Variable variable = table.get(name);
        if (variable == null) {
            if (parent == null) return 0.0;
            return parent.getValue(name, instance);
        }
        return variable.get(instance);
    }

    public void setValue(String name, ToDoubleFunction<MolangInstance> function) {
        Variable variable = table.get(name);
        if (variable == null) {
            table.put(name, new Variable(name, function));
        } else {
            variable.set(function);
        }
    }

    public void setValue(String name, Variable value) {
        Variable variable = table.get(name);
        if (variable == null) {
            table.put(name, value);
        } else {
            variable.set(value.value());
        }
    }

    public Variable computeIfAbsent(String name, Function<String, Variable> function) {
        Variable variable = table.get(name);
        if (variable == null) {
            if (parent == null) return function.apply(name);
            return parent.computeIfAbsent(name, function);
        }
        return variable;
    }

    public void setParent(@Nullable VariableTable parent) {
        this.parent = parent;
    }

    public @Nullable VariableTable getParent() {
        return parent;
    }
}

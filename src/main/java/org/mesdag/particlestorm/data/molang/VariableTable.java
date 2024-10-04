package org.mesdag.particlestorm.data.molang;

import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;

import java.util.Hashtable;
import java.util.function.ToDoubleFunction;

public class VariableTable {
    public final Hashtable<String, Variable> table;
    public @Nullable VariableTable previous;
    public @Nullable VariableTable subTable;

    public VariableTable(Hashtable<String, Variable> table, @Nullable VariableTable previous) {
        this.table = table;
        this.previous = previous;
    }

    public VariableTable(@Nullable VariableTable previous) {
        this(new Hashtable<>(), previous);
    }

    public double getValue(String name, MolangInstance instance) {
        Variable variable = table.get(name);
        if (variable == null) {
            if (subTable == null) {
                if (previous == null) return 0.0;
                return previous.getValue(name, instance);
            }
            return subTable.getValue(name, instance);
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
            variable.set(value);
        }
    }
}

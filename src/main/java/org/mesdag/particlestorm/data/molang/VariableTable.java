package org.mesdag.particlestorm.data.molang;

import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public class VariableTable {
    private static final ThreadLocal<Set<String>> RESOLVING = ThreadLocal.withInitial(HashSet::new);
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
        if (!beginResolve(name)) return 0.0;
        try {
            return variable.get(instance);
        } finally {
            endResolve(name);
        }
    }

    public double getLocalValue(String name, MolangInstance instance) {
        Variable variable = table.get(name);
        return variable == null ? 0.0 : variable.get(instance);
    }

    public void setValue(String name, ToDoubleFunction<MolangInstance> function) {
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
            if (parent == null) {
                variable = function.apply(name);
                table.put(name, variable);
                return variable;
            }
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

    protected boolean beginResolve(String name) {
        return RESOLVING.get().add(name);
    }

    protected void endResolve(String name) {
        Set<String> resolving = RESOLVING.get();
        resolving.remove(name);
        if (resolving.isEmpty()) {
            RESOLVING.remove();
        }
    }
}

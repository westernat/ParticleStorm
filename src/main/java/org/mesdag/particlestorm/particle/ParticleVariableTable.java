package org.mesdag.particlestorm.particle;

import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;

public class ParticleVariableTable extends VariableTable {
    private final VariableTable emitter;

    public ParticleVariableTable(VariableTable preset, VariableTable emitter) {
        super(preset);
        this.emitter = emitter;
    }

    @Override
    public double getValue(String name, MolangInstance instance) {
        Variable variable = table.get(name);
        if (variable == null) {
            variable = parent.table.get(name); // 预设表没有父级
            if (variable == null) {
                return emitter.getValue(name, instance);
            }
            return variable.get(instance);
        }
        return variable.get(instance);
    }
}

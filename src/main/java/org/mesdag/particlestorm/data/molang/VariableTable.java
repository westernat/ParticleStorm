package org.mesdag.particlestorm.data.molang;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

import java.util.function.ToDoubleFunction;

public class VariableTable {
    private final Object2ObjectAVLTreeMap<String, ParticleVariable> table;

    public VariableTable(Object2ObjectAVLTreeMap<String, ParticleVariable> table) {
        this.table = table;
    }

    public double getValue(String variable) {
        return table.get(variable).get();
    }

    public void setValue(String variable, ToDoubleFunction<MolangParticleInstance> function) {
        table.get(variable).set(function);
    }

    public void setValue(String variable, double value) {
        table.get(variable).set(instance -> value);
    }
}

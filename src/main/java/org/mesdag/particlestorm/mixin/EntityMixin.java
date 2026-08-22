package org.mesdag.particlestorm.mixin;

import net.minecraft.world.entity.Entity;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.mixed.IPSEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Hashtable;

@Mixin(Entity.class)
public abstract class EntityMixin implements IPSEntity {
    @Unique
    private VariableTable particlestorm$variableTable;

    @Override
    public VariableTable particlestorm$getVariableTable() {
        if (particlestorm$variableTable == null) {
            Hashtable<String, Variable> table = new Hashtable<>();
            table.put("variable.entity_scale", new Variable("variable.entity_scale", p -> 1));
            this.particlestorm$variableTable = new VariableTable(table, null);
        }
        return particlestorm$variableTable;
    }
}

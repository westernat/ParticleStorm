package org.mesdag.particlestorm.mixin;

import net.minecraft.world.entity.Entity;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.mixed.IEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Hashtable;

@Mixin(Entity.class)
public abstract class EntityMixin implements IEntity {
    @Unique
    private VariableTable particlestorm$variableTable;

    @Override
    public VariableTable particlestorm$getVariableTable() {
        if (particlestorm$variableTable == null) {
            this.particlestorm$variableTable = new VariableTable(new Hashtable<>(), null);
        }
        return particlestorm$variableTable;
    }
}

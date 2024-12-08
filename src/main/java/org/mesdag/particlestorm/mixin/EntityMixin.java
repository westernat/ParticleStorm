package org.mesdag.particlestorm.mixin;

import net.minecraft.world.entity.Entity;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.mixed.IEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityMixin implements IEntity {
    @Unique
    private final VariableTable particlestorm$variableTable = new VariableTable(INITIAL_TABLE);

    @Override
    public VariableTable particlestorm$getVariableTable() {
        return particlestorm$variableTable;
    }
}

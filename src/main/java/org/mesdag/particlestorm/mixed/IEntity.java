package org.mesdag.particlestorm.mixed;

import net.minecraft.world.entity.Entity;
import org.mesdag.particlestorm.data.molang.VariableTable;

public interface IEntity {
    VariableTable particlestorm$getVariableTable();

    static IEntity of(Entity entity) {
        return (IEntity) entity;
    }
}

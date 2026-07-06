package org.mesdag.particlestorm.mixed;

import net.minecraft.world.entity.Entity;
import org.mesdag.particlestorm.data.molang.VariableTable;

public interface IPSEntity {
    VariableTable particlestorm$getVariableTable();

    static IPSEntity of(Entity entity) {
        return (IPSEntity) entity;
    }
}

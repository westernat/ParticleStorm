package org.mesdag.particlestorm.mixed;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.mesdag.particlestorm.data.molang.VariableTable;

public interface IPSBlockEntity {
    VariableTable particlestorm$getVariableTable();

    static IPSBlockEntity of(BlockEntity entity) {
        return (IPSBlockEntity) entity;
    }
}

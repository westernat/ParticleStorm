package org.mesdag.particlestorm.mixed;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.mesdag.particlestorm.data.molang.VariableTable;

public interface IBlockEntity {
    VariableTable particlestorm$getVariableTable();

    static IBlockEntity of(BlockEntity blockEntity) {
        return (IBlockEntity) blockEntity;
    }
}

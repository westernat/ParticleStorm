package org.mesdag.particlestorm.mixin;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.mixed.IBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements IBlockEntity {
    @Unique
    private VariableTable particlestorm$variableTable;

    @Override
    public VariableTable particlestorm$getVariableTable() {
        if (particlestorm$variableTable == null) {
            this.particlestorm$variableTable = new VariableTable(null);
        }
        return particlestorm$variableTable;
    }
}

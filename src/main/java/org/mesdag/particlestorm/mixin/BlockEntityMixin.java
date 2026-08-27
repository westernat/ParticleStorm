package org.mesdag.particlestorm.mixin;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import org.mesdag.particlestorm.api.AddDefaultVariableEvent;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.mixed.IPSBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements IPSBlockEntity {
    @Unique
    private VariableTable particlestorm$variableTable;

    @Override
    public VariableTable particlestorm$getVariableTable() {
        if (particlestorm$variableTable == null) {
            this.particlestorm$variableTable = new VariableTable(null);
            NeoForge.EVENT_BUS.post(new AddDefaultVariableEvent.BlockEntity(particlestorm$variableTable, (BlockEntity) (Object) this));
        }
        return particlestorm$variableTable;
    }
}

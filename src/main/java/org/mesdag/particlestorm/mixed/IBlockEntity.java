package org.mesdag.particlestorm.mixed;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.ParticleEmitterAttachable;
import org.mesdag.particlestorm.data.molang.VariableTable;

public interface IBlockEntity extends ParticleEmitterAttachable {
    VariableTable particlestorm$getVariableTable();

    @Override
    default Vec3 getPos() {
        var pos = ((BlockEntity) this).getBlockPos();
        return new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    @Override
    default @Nullable Level getLevel() {
        return ((BlockEntity) this).getLevel();
    }

    @Override
    default boolean isDiscarded() {
        return ((BlockEntity) this).isRemoved();
    }

    @Override
    default VariableTable getVariableTable() {
        return particlestorm$getVariableTable();
    }

    static IBlockEntity of(BlockEntity blockEntity) {
        return (IBlockEntity) blockEntity;
    }
}

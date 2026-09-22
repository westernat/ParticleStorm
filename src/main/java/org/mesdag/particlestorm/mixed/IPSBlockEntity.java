package org.mesdag.particlestorm.mixed;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.ParticleEmitterAttachable;
import org.mesdag.particlestorm.data.molang.VariableTable;

public interface IPSBlockEntity extends ParticleEmitterAttachable {
    private net.minecraft.world.level.block.entity.@NotNull BlockEntity self() {
        return (net.minecraft.world.level.block.entity.BlockEntity) this;
    }

    @Override
    default Vec3 getPos() {
        return Vec3.atBottomCenterOf(self().getBlockPos());
    }

    @Override
    default @Nullable Level getLevel() {
        return self().getLevel();
    }

    @Override
    default boolean isDiscarded() {
        return self().isRemoved();
    }

    @Override
    default VariableTable getVariableTable() {
        return particlestorm$getVariableTable();
    }

    VariableTable particlestorm$getVariableTable();

    static IPSBlockEntity of(net.minecraft.world.level.block.entity.BlockEntity entity) {
        return (IPSBlockEntity) entity;
    }
}

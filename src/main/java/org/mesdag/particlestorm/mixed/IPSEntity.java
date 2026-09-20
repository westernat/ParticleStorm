package org.mesdag.particlestorm.mixed;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.mesdag.particlestorm.api.ParticleEmitterAttachable;
import org.mesdag.particlestorm.data.molang.VariableTable;

public interface IPSEntity extends ParticleEmitterAttachable {
    private net.minecraft.world.entity.Entity self() {
        return (net.minecraft.world.entity.Entity) this;
    }

    @Override
    default Vec3 getPos() {
        return self().position();
    }

    @Override
    default Level getLevel() {
        return self().level();
    }

    @Override
    default boolean isDiscarded() {
        return self().isRemoved();
    }

    @Override
    default VariableTable getVariableTable() {
        return ((IPSEntity) this).particlestorm$getVariableTable();
    }

    VariableTable particlestorm$getVariableTable();

    static IPSEntity of(net.minecraft.world.entity.Entity entity) {
        return (IPSEntity) entity;
    }
}

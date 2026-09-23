package org.mesdag.particlestorm.mixed;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.mesdag.particlestorm.api.ParticleEmitterAttachable;
import org.mesdag.particlestorm.data.molang.VariableTable;

public interface IEntity extends ParticleEmitterAttachable {
    VariableTable particlestorm$getVariableTable();

    @Override
    default Vec3 getPos() {
        return ((Entity) this).position();
    }

    @Override
    default Level getLevel() {
        return ((Entity) this).level();
    }

    @Override
    default boolean isDiscarded() {
        return ((Entity) this).isRemoved();
    }

    @Override
    default VariableTable getVariableTable() {
        return particlestorm$getVariableTable();
    }

    static IEntity of(Entity entity) {
        return (IEntity) entity;
    }
}

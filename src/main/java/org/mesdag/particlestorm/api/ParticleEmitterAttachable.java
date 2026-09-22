package org.mesdag.particlestorm.api;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.data.molang.VariableTable;

public interface ParticleEmitterAttachable {
    Vec3 getPos();

    @Nullable Level getLevel();

    boolean isDiscarded();

    VariableTable getVariableTable();
}

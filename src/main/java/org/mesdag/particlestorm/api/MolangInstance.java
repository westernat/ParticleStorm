package org.mesdag.particlestorm.api;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.particle.ParticleEmitter;

public interface MolangInstance {
    VariableTable getVars();

    Level getLevel();

    float tickAge();

    float tickLifetime();

    float getRandom1();

    float getRandom2();

    float getRandom3();

    float getRandom4();

    ResourceLocation getIdentity();

    Vec3 getPosition();

    @Nullable Entity getAttachedEntity();

    float getInvTickRate();

    ParticleEmitter getEmitter();
}

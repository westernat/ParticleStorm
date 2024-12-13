package org.mesdag.particlestorm.api;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.particle.ParticleEmitter;

public interface MolangInstance {
    VariableTable getVariableTable();

    Level getLevel();

    float tickAge();

    float tickLifetime();

    double getRandom1();

    double getRandom2();

    double getRandom3();

    double getRandom4();

    ResourceLocation getIdentity();

    Vec3 getPosition();

    Entity getAttachedEntity();

    float getInvTickRate();

    ParticleEmitter getEmitter();
}

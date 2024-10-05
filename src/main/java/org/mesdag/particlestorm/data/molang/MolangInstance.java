package org.mesdag.particlestorm.data.molang;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

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
}

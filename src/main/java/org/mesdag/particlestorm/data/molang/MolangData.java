package org.mesdag.particlestorm.data.molang;


import net.minecraft.world.level.Level;

public interface MolangData {
    VariableTable getVariableTable();

    Level getLevel();

    int getAge();

    int getLifetime();

    double getRandom1();

    double getRandom2();

    double getRandom3();

    double getRandom4();
}

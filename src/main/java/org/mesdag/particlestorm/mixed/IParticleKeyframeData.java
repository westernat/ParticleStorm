package org.mesdag.particlestorm.mixed;

import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.VariableTable;

public interface IParticleKeyframeData {
    ResourceLocation particlestorm$getParticle();

    MolangExp particlestorm$getExpression(VariableTable variableTable);

    int[] particlestorm$getCachedId(int size);
}

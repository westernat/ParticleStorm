package org.mesdag.particlestorm.mixed;

import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.VariableTable;
import software.bernie.geckolib.core.keyframe.event.data.ParticleKeyframeData;

public interface IParticleKeyframeData {
    ResourceLocation particlestorm$getParticle();

    MolangExp particlestorm$getExpression(VariableTable variableTable);

    static IParticleKeyframeData of(ParticleKeyframeData data) {
        return (IParticleKeyframeData) data;
    }
}

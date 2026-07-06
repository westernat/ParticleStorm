package org.mesdag.particlestorm.mixed;

import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.VariableTable;
import software.bernie.geckolib.animation.keyframe.event.data.ParticleKeyframeData;

public interface IPSParticleKeyframeData {
    ResourceLocation particlestorm$getParticle();

    MolangExp particlestorm$getExpression(VariableTable variableTable);

    static IPSParticleKeyframeData of(ParticleKeyframeData data) {
        return (IPSParticleKeyframeData) data;
    }
}

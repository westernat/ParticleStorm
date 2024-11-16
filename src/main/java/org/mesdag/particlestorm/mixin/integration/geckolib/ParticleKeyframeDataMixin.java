package org.mesdag.particlestorm.mixin.integration.geckolib;

import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.data.molang.compiler.MolangParser;
import org.mesdag.particlestorm.mixed.IParticleKeyframeData;
import org.spongepowered.asm.mixin.*;

import java.util.Arrays;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.animation.keyframe.event.data.ParticleKeyframeData", remap = false)
public abstract class ParticleKeyframeDataMixin implements IParticleKeyframeData {
    @Shadow
    @Final
    private String effect;
    @Shadow
    @Final
    private String script;

    @Unique
    private ResourceLocation particlestorm$particle;
    @Unique
    private MolangExp particlestorm$expression;
    @Unique
    private int[] particlestorm$cachedId;

    @Override
    public ResourceLocation particlestorm$getParticle() {
        if (particlestorm$particle == null) {
            this.particlestorm$particle = ResourceLocation.parse(effect);
        }
        return particlestorm$particle;
    }

    @Override
    public MolangExp particlestorm$getExpression(VariableTable variableTable) {
        if (particlestorm$expression == null) {
            this.particlestorm$expression = new MolangExp(script);
            particlestorm$expression.compile(new MolangParser(variableTable));
        }
        return particlestorm$expression;
    }

    @Override
    public int[] particlestorm$getCachedId(int size) {
        if (particlestorm$cachedId == null) {
            this.particlestorm$cachedId = new int[size];
            Arrays.fill(particlestorm$cachedId, -1);
        }
        return particlestorm$cachedId;
    }
}

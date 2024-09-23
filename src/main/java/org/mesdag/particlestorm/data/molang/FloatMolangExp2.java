package org.mesdag.particlestorm.data.molang;

import com.mojang.serialization.Codec;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

import java.util.List;

public record FloatMolangExp2(FloatMolangExp exp1, FloatMolangExp exp2) {
    public static final FloatMolangExp2 ZERO = new FloatMolangExp2(FloatMolangExp.ZERO, FloatMolangExp.ZERO);
    public static final Codec<FloatMolangExp2> CODEC = Codec.list(FloatMolangExp.CODEC, 2, 2).xmap(
            exps -> new FloatMolangExp2(exps.getFirst(), exps.get(1)),
            exp2 -> List.of(exp2.exp1, exp2.exp2)
    );

    public float[] calculate(MolangParticleInstance instance) {
        return new float[]{exp1.calculate(instance), exp2.calculate(instance)};
    }

    public boolean initialized() {
        return exp1.initialized() && exp2.initialized();
    }
}

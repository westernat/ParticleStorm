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

    public double[] calculate(MolangParticleInstance instance) {
        return new double[]{exp1.variable.get(instance), exp2.variable.get(instance)};
    }

    public boolean initialized() {
        return exp1.initialized() && exp2.initialized();
    }
}

package org.mesdag.particlestorm.data.molang;

import com.mojang.serialization.Codec;

import java.util.List;

public record FloatMolangExp3(FloatMolangExp exp1, FloatMolangExp exp2, FloatMolangExp exp3) {
    public static final FloatMolangExp3 ZERO = new FloatMolangExp3(FloatMolangExp.ZERO, FloatMolangExp.ZERO, FloatMolangExp.ZERO);
    public static final FloatMolangExp3 X = new FloatMolangExp3(FloatMolangExp.ONE, FloatMolangExp.ZERO, FloatMolangExp.ZERO);
    public static final FloatMolangExp3 Y = new FloatMolangExp3(FloatMolangExp.ZERO, FloatMolangExp.ONE, FloatMolangExp.ZERO);
    public static final FloatMolangExp3 Z = new FloatMolangExp3(FloatMolangExp.ZERO, FloatMolangExp.ZERO, FloatMolangExp.ONE);
    public static final Codec<FloatMolangExp3> CODEC = Codec.list(FloatMolangExp.CODEC, 3, 3).xmap(
            exps -> new FloatMolangExp3(exps.getFirst(), exps.get(1), exps.get(2)),
            exp3 -> List.of(exp3.exp1, exp3.exp2, exp3.exp3)
    );

    public float[] calculate(MolangData instance) {
        return new float[]{exp1.calculate(instance), exp2.calculate(instance), exp3.calculate(instance)};
    }

    @Override
    public String toString() {
        return "FloatMolangExp3{" +
                "exp1=" + exp1 +
                ", exp2=" + exp2 +
                ", exp3=" + exp3 +
                '}';
    }
}

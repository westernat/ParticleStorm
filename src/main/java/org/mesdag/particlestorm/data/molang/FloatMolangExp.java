package org.mesdag.particlestorm.data.molang;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import org.mesdag.particlestorm.api.MolangInstance;

public class FloatMolangExp extends MolangExp {
    public static final FloatMolangExp ZERO = FloatMolangExp.ofConstant(0);
    public static final FloatMolangExp ONE = FloatMolangExp.ofConstant(1);
    public static final Codec<FloatMolangExp> CODEC = Codec.either(Codec.FLOAT, Codec.STRING).xmap(
            either -> either.map(f -> new FloatMolangExp(f, ""), s -> new FloatMolangExp(0.0F, s)),
            molang -> molang.expStr.isEmpty() ? Either.left(molang.constant) : Either.right(molang.expStr)
    );
    private final float constant;

    public FloatMolangExp(float constant, String expression) {
        super(expression);
        this.constant = constant;
    }

    public float getConstant() {
        return constant;
    }

    @Override
    public boolean initialized() {
        return constant != 0.0F || super.initialized();
    }

    @Override
    public float calculate(MolangInstance instance) {
        if (!initialized()) return 0.0F;
        return variable == null ? constant : (float) variable.get(instance);
    }

    @Override
    public String toString() {
        return "FloatMolangExp{" + (expStr.isEmpty() ? constant : expStr) + '}';
    }

    public static FloatMolangExp ofConstant(float constant) {
        return new FloatMolangExp(constant, "");
    }

    public static FloatMolangExp ofExpression(String expression) {
        return new FloatMolangExp(0.0F, expression);
    }
}

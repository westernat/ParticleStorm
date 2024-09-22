package org.mesdag.particlestorm.data.molang;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.Nullable;

public class BoolMolangExp extends MolangExp {
    public static final BoolMolangExp TRUE = new BoolMolangExp(true, null);
    public static final BoolMolangExp FALSE = new BoolMolangExp(false, null);
    public static final Codec<BoolMolangExp> CODEC = Codec.either(Codec.BOOL, Codec.STRING).xmap(
            either -> either.map(b -> new BoolMolangExp(b, null), e -> new BoolMolangExp(true, e)),
            molang -> molang.expStr.isEmpty() ? Either.left(molang.constant) : Either.right(molang.expStr)
    );
    private final boolean constant;

    public BoolMolangExp(boolean constant, @Nullable String expression) {
        super(expression);
        this.constant = constant;
    }

    public boolean getConstant() {
        return constant;
    }

    public static BoolMolangExp ofExpression(String expression) {
        return new BoolMolangExp(true, expression);
    }
}

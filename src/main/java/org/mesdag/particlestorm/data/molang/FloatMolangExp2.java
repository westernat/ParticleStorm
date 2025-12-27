package org.mesdag.particlestorm.data.molang;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.MolangInstance;

public record FloatMolangExp2(FloatMolangExp exp1, FloatMolangExp exp2) {
    public static final FloatMolangExp2 ZERO = new FloatMolangExp2(FloatMolangExp.ZERO, FloatMolangExp.ZERO);

    public float[] calculate(MolangInstance instance) {
        return new float[]{exp1.calculate(instance), exp2.calculate(instance)};
    }

    @Deprecated
    public boolean initialized() {
        return exp1.initialized() && exp2.initialized();
    }

    @Override
    public String toString() {
        return "FloatMolangExp2{" +
                "exp1=" + exp1 +
                ", exp2=" + exp2 +
                '}';
    }

    public static FloatMolangExp2 fromJson(@Nullable JsonElement element, FloatMolangExp2 defaultValue) {
        if (element == null) return defaultValue;
        JsonArray array = element.getAsJsonArray();
        if (array.size() != 2) {
            throw new JsonParseException("Length of array must be 2: " + array);
        }
        return new FloatMolangExp2(
                FloatMolangExp.fromJson(array.get(0)),
                FloatMolangExp.fromJson(array.get(1))
        );
    }

    public static FloatMolangExp2 fromJson(@Nullable JsonElement element) {
        return fromJson(element, ZERO);
    }
}

package org.mesdag.particlestorm.data.molang;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.MolangInstance;

public record FloatMolangExp3(FloatMolangExp exp1, FloatMolangExp exp2, FloatMolangExp exp3) {
    public static final FloatMolangExp3 ZERO = new FloatMolangExp3(FloatMolangExp.ZERO, FloatMolangExp.ZERO, FloatMolangExp.ZERO);
    public static final FloatMolangExp3 X = new FloatMolangExp3(FloatMolangExp.ONE, FloatMolangExp.ZERO, FloatMolangExp.ZERO);
    public static final FloatMolangExp3 Y = new FloatMolangExp3(FloatMolangExp.ZERO, FloatMolangExp.ONE, FloatMolangExp.ZERO);
    public static final FloatMolangExp3 Z = new FloatMolangExp3(FloatMolangExp.ZERO, FloatMolangExp.ZERO, FloatMolangExp.ONE);

    public float[] calculate(MolangInstance instance) {
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

    public static FloatMolangExp3 fromJson(@Nullable JsonElement element, FloatMolangExp3 defaultValue) {
        if (element == null) return defaultValue;
        JsonArray array = element.getAsJsonArray();
        if (array.size() != 3) {
            throw new JsonParseException("Length of array must be 3: " + array);
        }
        return new FloatMolangExp3(
                FloatMolangExp.fromJson(array.get(0)),
                FloatMolangExp.fromJson(array.get(1)),
                FloatMolangExp.fromJson(array.get(2))
        );
    }

    public static FloatMolangExp3 fromJson(@Nullable JsonElement element) {
        return fromJson(element, ZERO);
    }
}

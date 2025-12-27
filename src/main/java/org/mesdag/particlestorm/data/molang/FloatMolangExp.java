package org.mesdag.particlestorm.data.molang;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.MolangInstance;

public class FloatMolangExp extends MolangExp {
    public static final FloatMolangExp ZERO = FloatMolangExp.ofConstant(0);
    public static final FloatMolangExp ONE = FloatMolangExp.ofConstant(1);
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

    public static FloatMolangExp fromJson(@Nullable JsonElement element, FloatMolangExp defaultValue) {
        if (element == null) return defaultValue;
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isString()) {
            return ofExpression(primitive.getAsString());
        }
        if (primitive.isNumber()) {
            return ofConstant(primitive.getAsFloat());
        }
        throw new JsonParseException("Not a string or float: " + element);
    }

    public static FloatMolangExp fromJson(@Nullable JsonElement element) {
        return fromJson(element, ZERO);
    }
}

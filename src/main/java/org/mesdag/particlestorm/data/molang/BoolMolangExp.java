package org.mesdag.particlestorm.data.molang;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import org.mesdag.particlestorm.api.MolangInstance;

public class BoolMolangExp extends MolangExp {
    public static final BoolMolangExp TRUE = new BoolMolangExp(true, "");
    public static final BoolMolangExp FALSE = new BoolMolangExp(false, "");
    private final boolean constant;

    public BoolMolangExp(boolean constant, String expression) {
        super(expression);
        this.constant = constant;
    }

    public boolean getConstant() {
        return constant;
    }

    public boolean get(MolangInstance instance) {
        if (!initialized()) return false;
        return variable == null ? constant : variable.get(instance) != 0.0;
    }

    @Override
    public boolean initialized() {
        return constant || variable != null;
    }

    @Override
    public String toString() {
        return "BoolMolangExp{" + (expStr.isEmpty() ? constant : expStr) + '}';
    }

    public static BoolMolangExp ofExpression(String expression) {
        return new BoolMolangExp(true, expression);
    }

    public static BoolMolangExp fromJson(JsonElement element) {
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return new BoolMolangExp(primitive.getAsBoolean(), "");
        }
        if (primitive.isString()) {
            return new BoolMolangExp(true, primitive.getAsString());
        }
        throw new JsonParseException("Not a boolean or string: " + element);
    }
}

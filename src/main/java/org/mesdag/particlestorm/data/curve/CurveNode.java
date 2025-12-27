package org.mesdag.particlestorm.data.curve;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public record CurveNode(float value, float slope) {
    public static CurveNode fromJson(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        float value = GsonHelper.getAsFloat(object, "value", 1.0F);
        float slope = GsonHelper.getAsFloat(object, "slope", 1.0F);
        return new CurveNode(value, slope);
    }
}

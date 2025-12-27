package org.mesdag.particlestorm.data.curve;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Either;
import net.minecraft.util.Tuple;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class CurveNodes {
    public final Either<Map<String, CurveNode>, List<FloatMolangExp>> either;
    public final boolean isLeft;

    public final ArrayList<Tuple<Float, CurveNode>> nodeList;

    public CurveNodes(Either<Map<String, CurveNode>, List<FloatMolangExp>> either) {
        this.either = either;
        this.isLeft = either.left().isPresent();

        this.nodeList = new ArrayList<>();
        if (isLeft) {
            either.left().get().entrySet().stream()
                    .map(entry -> new Tuple<>(Float.parseFloat(entry.getKey()), entry.getValue()))
                    .sorted(Comparator.comparing(Tuple::getA))
                    .forEachOrdered(nodeList::add);
        }
    }

    public int length() {
        if (isLeft) return either.left().get().size();
        return either.right().get().size();
    }

    public static CurveNodes fromJson(JsonElement element, CurveType type) {
        if (element.isJsonObject()) {
            if (type == CurveType.LINEAR) {
                throw new JsonParseException("Curve type must not be linear");
            }
            JsonObject object = element.getAsJsonObject();
            ImmutableMap.Builder<String, CurveNode> builder = ImmutableMap.builder();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                builder.put(entry.getKey(), CurveNode.fromJson(entry.getValue()));
            }
            return new CurveNodes(Either.left(builder.build()));
        }
        if (element.isJsonArray()) {
            if (type != CurveType.LINEAR) {
                throw new JsonParseException("Curve type must be linear");
            }
            JsonArray array = element.getAsJsonArray();
            ImmutableList.Builder<FloatMolangExp> builder = ImmutableList.builder();
            for (JsonElement jsonElement : array) {
                builder.add(FloatMolangExp.fromJson(jsonElement));
            }
            return new CurveNodes(Either.right(builder.build()));
        }
        throw new JsonParseException("Not a object or array: " + element);
    }
}

package org.mesdag.particlestorm.data.curve;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CurveType {
    CATMULL_ROM,
    LINEAR,
    BEZIER,
    BEZIER_CHAIN;

    private static final Map<String, CurveType> MAP = Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(
            e -> e.name().toLowerCase(Locale.ROOT),
            Function.identity()
    ));

    public static CurveType fromJson(@Nullable JsonElement element) {
        if (element == null) return LINEAR;
        return MAP.getOrDefault(element.getAsString(), LINEAR);
    }
}

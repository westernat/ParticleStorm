package org.mesdag.particlestorm.data.description;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum DescriptionMaterial {
    TERRAIN_SHEET,
    PARTICLE_SHEET_OPAQUE,
    PARTICLE_SHEET_TRANSLUCENT,
    PARTICLE_SHEET_LIT,
    CUSTOM,
    NO_RENDER,

    particles_alpha,
    particles_blend,
    particles_add,
    particles_opaque;

    private static final Map<String, DescriptionMaterial> MAP = Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(
            e -> e.name().toLowerCase(Locale.ROOT),
            Function.identity()
    ));

    public static DescriptionMaterial fromJson(@Nullable JsonElement element) {
        if (element == null) return CUSTOM;
        return MAP.getOrDefault(element.getAsString(), CUSTOM);
    }
}

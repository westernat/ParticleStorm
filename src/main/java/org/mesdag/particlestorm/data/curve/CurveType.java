package org.mesdag.particlestorm.data.curve;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum CurveType implements StringRepresentable {
    CATMULL_ROM,
    LINEAR,
    BEZIER,
    BEZIER_CHAIN;

    public static final Codec<CurveType> CODEC = StringRepresentable.fromEnum(CurveType::values);

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

}

package org.mesdag.particlestorm.data.description;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum DescriptionMaterial implements StringRepresentable {
    TERRAIN_SHEET,
    PARTICLE_SHEET_OPAQUE,
    PARTICLE_SHEET_TRANSLUCENT,
    PARTICLE_SHEET_LIT,
    CUSTOM,
    NO_RENDER;

    public static final Codec<DescriptionMaterial> CODEC = StringRepresentable.fromEnum(DescriptionMaterial::values);

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

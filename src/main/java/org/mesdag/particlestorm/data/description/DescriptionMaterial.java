package org.mesdag.particlestorm.data.description;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.fml.ModLoader;
import org.mesdag.particlestorm.api.RegisterCustomMaterialEvent;

import java.util.ArrayList;
import java.util.List;

public final class DescriptionMaterial implements StringRepresentable {
    private static List<DescriptionMaterial> list = new ArrayList<>();
    private static DescriptionMaterial[] values;

    public static final DescriptionMaterial
            TERRAIN_SHEET = register("terrain_sheet"),
            PARTICLE_SHEET_OPAQUE = register("particle_sheet_opaque"),
            PARTICLE_SHEET_TRANSLUCENT = register("particle_sheet_translucent"),
            PARTICLE_SHEET_LIT = register("particle_sheet_lit"),
            CUSTOM = register("custom"),
            NO_RENDER = register("no_renderer"),
            particles_alpha = register("particles_alpha"),
            particles_blend = register("particles_blend"),
            particles_add = register("particles_add"),
            particles_opaque = register("particles_opaque");


    private static DescriptionMaterial register(String name) {
        DescriptionMaterial material = new DescriptionMaterial(name);
        list.add(material);
        return material;
    }

    public static final Codec<DescriptionMaterial> CODEC = StringRepresentable.fromValues(() -> {
        if (values == null) {
            ModLoader.postEvent(new RegisterCustomMaterialEvent(DescriptionMaterial::register));
            values = list.toArray(DescriptionMaterial[]::new);
            list = null;
        }
        return values;
    });

    private final String name;

    private DescriptionMaterial(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}

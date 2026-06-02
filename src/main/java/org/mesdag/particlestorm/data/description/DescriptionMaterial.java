package org.mesdag.particlestorm.data.description;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.Util;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.fml.ModLoader;
import org.mesdag.particlestorm.api.RegisterCustomMaterialEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DescriptionMaterial implements StringRepresentable {
    private static List<DescriptionMaterial> list = new ArrayList<>();

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

    public static final Codec<DescriptionMaterial> CODEC = new Codec<>() {
        private final Map<String, DescriptionMaterial> getter = Util.make(new HashMap<>(), map -> {
            ModLoader.get().postEvent(new RegisterCustomMaterialEvent(DescriptionMaterial::register));
            for (DescriptionMaterial material : list) {
                map.put(material.getSerializedName(), material);
            }
            list = null;
        });

        @Override
        public <T> DataResult<Pair<DescriptionMaterial, T>> decode(DynamicOps<T> ops, T input) {
            return Codec.STRING.decode(ops, input).map(p -> new Pair<>(getter.getOrDefault(p.getFirst(), CUSTOM), p.getSecond()));
        }

        @Override
        public <T> DataResult<T> encode(DescriptionMaterial input, DynamicOps<T> ops, T prefix) {
            return Codec.STRING.encode(input.getSerializedName(), ops, prefix);
        }
    };

    private final String name;

    private DescriptionMaterial(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}

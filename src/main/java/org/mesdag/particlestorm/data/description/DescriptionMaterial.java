package org.mesdag.particlestorm.data.description;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.StringRepresentable;
import org.mesdag.particlestorm.api.RegisterCustomMaterialEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DescriptionMaterial implements StringRepresentable {
    private static List<DescriptionMaterial> pending = new ArrayList<>();
    private static Map<String, DescriptionMaterial> values;

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

    private final String name;
    public static final Codec<DescriptionMaterial> CODEC = Codec.STRING.comapFlatMap(
            name -> {
                DescriptionMaterial material = registeredValues().get(name);
                return material == null ? DataResult.error(() -> "Unknown particle material: " + name) : DataResult.success(material);
            },
            DescriptionMaterial::getSerializedName
    );

    private DescriptionMaterial(String name) {
        this.name = name;
    }

    private static DescriptionMaterial register(String name) {
        if (pending == null) {
            throw new IllegalStateException("Particle materials are already frozen");
        }
        DescriptionMaterial material = new DescriptionMaterial(name);
        pending.add(material);
        return material;
    }

    private static synchronized Map<String, DescriptionMaterial> registeredValues() {
        if (values == null) {
            fireRegistrationEvent();
            Map<String, DescriptionMaterial> materialMap = new HashMap<>();
            for (DescriptionMaterial material : pending) {
                materialMap.put(material.name, material);
            }
            materialMap.put("no_render", NO_RENDER);
            values = Map.copyOf(materialMap);
            pending = null;
        }
        return values;
    }

    private static void fireRegistrationEvent() {
        RegisterCustomMaterialEvent.EVENT.invoker().onRegister(new RegisterCustomMaterialEvent(DescriptionMaterial::register));
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}

package org.mesdag.particlestorm.api;

import com.google.common.collect.HashBiMap;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.List;

public interface IComponent {
    HashBiMap<ResourceLocation, Deserializer<IComponent>> COMPONENTS = HashBiMap.create();

    @SuppressWarnings("unchecked")
    static void register(ResourceLocation id, Deserializer<? extends IComponent> codec) {
        COMPONENTS.put(id, (Deserializer<IComponent>) codec);
    }

    static void register(String vanillaPath, Deserializer<? extends IComponent> codec) {
        register(new ResourceLocation(vanillaPath), codec);
    }

    Deserializer<? extends IComponent> deserializer();

    List<MolangExp> getAllMolangExp();

    /// @return <= 0 means early initialize
    default int order() {
        return 1000;
    }
}

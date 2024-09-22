package org.mesdag.particlestorm.data.component;

import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.List;

public interface IComponent {
    HashBiMap<ResourceLocation, Codec<IComponent>> COMPONENTS = HashBiMap.create();

    @SuppressWarnings("unchecked")
    static void register(ResourceLocation id, Codec<? extends IComponent> codec) {
        COMPONENTS.put(id, (Codec<IComponent>) codec);
    }

    static void register(String vanillaPath, Codec<? extends IComponent> codec) {
        register(ResourceLocation.withDefaultNamespace(vanillaPath), codec);
    }

    Codec<? extends IComponent> codec();

    List<MolangExp> getAllMolangExp();
}

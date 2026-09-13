package org.mesdag.particlestorm.api;

import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.List;

public interface IComponent {
    HashBiMap<Identifier, Codec<IComponent>> COMPONENTS = HashBiMap.create();

    @SuppressWarnings("unchecked")
    static void register(Identifier id, Codec<? extends IComponent> codec) {
        COMPONENTS.put(id, (Codec<IComponent>) codec);
    }

    static void register(String vanillaPath, Codec<? extends IComponent> codec) {
        register(Identifier.withDefaultNamespace(vanillaPath), codec);
    }

    Codec<? extends IComponent> codec();

    List<MolangExp> getAllMolangExp();

    /// @return order < 0 means early initialize and no update
    default int order() {
        return 1000;
    }
}

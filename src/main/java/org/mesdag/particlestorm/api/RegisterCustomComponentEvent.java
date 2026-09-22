package org.mesdag.particlestorm.api;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

public class RegisterCustomComponentEvent {
    public RegisterCustomComponentEvent() {
    }

    public void register(ResourceLocation id, Codec<? extends IComponent> codec) {
        IComponent.register(id, codec);
    }

    public void register(String vanillaPath, Codec<? extends IComponent> codec) {
        IComponent.register(vanillaPath, codec);
    }
}

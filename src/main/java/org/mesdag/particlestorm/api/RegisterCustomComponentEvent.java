package org.mesdag.particlestorm.api;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;

public class RegisterCustomComponentEvent extends Event implements IModBusEvent {
    public RegisterCustomComponentEvent() {
    }

    public void register(Identifier id, Codec<? extends IComponent> codec) {
        IComponent.register(id, codec);
    }

    public void register(String vanillaPath, Codec<? extends IComponent> codec) {
        IComponent.register(vanillaPath, codec);
    }
}

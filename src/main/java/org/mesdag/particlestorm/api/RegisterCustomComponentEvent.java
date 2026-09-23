package org.mesdag.particlestorm.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;

public class RegisterCustomComponentEvent {
    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class, listeners -> event -> {
        for (Callback listener : listeners) {
            listener.onRegister(event);
        }
    });

    @FunctionalInterface
    public interface Callback {
        void onRegister(RegisterCustomComponentEvent event);
    }

    public RegisterCustomComponentEvent() {
    }

    public void register(Identifier id, Codec<? extends IComponent> codec) {
        IComponent.register(id, codec);
    }

    public void register(String vanillaPath, Codec<? extends IComponent> codec) {
        IComponent.register(vanillaPath, codec);
    }
}

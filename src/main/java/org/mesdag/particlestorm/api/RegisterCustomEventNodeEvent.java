package org.mesdag.particlestorm.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import com.mojang.serialization.Codec;

public class RegisterCustomEventNodeEvent {
    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class, listeners -> event -> {
        for (Callback listener : listeners) {
            listener.onRegister(event);
        }
    });

    @FunctionalInterface
    public interface Callback {
        void onRegister(RegisterCustomEventNodeEvent event);
    }

    public RegisterCustomEventNodeEvent() {
    }

    public void register(String name, Codec<? extends IEventNode> codec) {
        IEventNode.register(name, codec);
    }
}

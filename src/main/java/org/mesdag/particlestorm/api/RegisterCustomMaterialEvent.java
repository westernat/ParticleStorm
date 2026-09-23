package org.mesdag.particlestorm.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import org.mesdag.particlestorm.data.description.DescriptionMaterial;

import java.util.function.Function;

public class RegisterCustomMaterialEvent {
    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class, listeners -> event -> {
        for (Callback listener : listeners) {
            listener.onRegister(event);
        }
    });

    @FunctionalInterface
    public interface Callback {
        void onRegister(RegisterCustomMaterialEvent event);
    }

    private final Function<String, DescriptionMaterial> factory;

    public RegisterCustomMaterialEvent(Function<String, DescriptionMaterial> factory) {
        this.factory = factory;
    }

    public DescriptionMaterial register(String name) {
        return factory.apply(name);
    }
}

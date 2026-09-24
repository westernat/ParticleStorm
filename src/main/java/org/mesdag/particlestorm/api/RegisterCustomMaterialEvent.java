package org.mesdag.particlestorm.api;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.mesdag.particlestorm.data.description.DescriptionMaterial;

import java.util.function.Function;

public class RegisterCustomMaterialEvent extends Event implements IModBusEvent {
    private final Function<String, DescriptionMaterial> factory;

    public RegisterCustomMaterialEvent(Function<String, DescriptionMaterial> factory) {
        this.factory = factory;
    }

    public DescriptionMaterial register(String name) {
        return factory.apply(name);
    }
}

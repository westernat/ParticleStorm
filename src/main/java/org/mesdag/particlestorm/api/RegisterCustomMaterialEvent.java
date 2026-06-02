package org.mesdag.particlestorm.api;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import org.mesdag.particlestorm.data.description.DescriptionMaterial;

import java.util.function.Function;

public class RegisterCustomMaterialEvent extends Event implements IModBusEvent {
    private final Function<String, DescriptionMaterial> consumer;

    public RegisterCustomMaterialEvent(Function<String, DescriptionMaterial> function) {
        this.consumer = function;
    }

    public DescriptionMaterial register(String name) {
        return consumer.apply(name);
    }
}

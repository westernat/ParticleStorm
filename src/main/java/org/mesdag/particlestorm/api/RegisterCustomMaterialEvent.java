package org.mesdag.particlestorm.api;

import org.mesdag.particlestorm.data.description.DescriptionMaterial;

import java.util.function.Function;

public class RegisterCustomMaterialEvent {
    private final Function<String, DescriptionMaterial> factory;

    public RegisterCustomMaterialEvent(Function<String, DescriptionMaterial> factory) {
        this.factory = factory;
    }

    public DescriptionMaterial register(String name) {
        return factory.apply(name);
    }
}

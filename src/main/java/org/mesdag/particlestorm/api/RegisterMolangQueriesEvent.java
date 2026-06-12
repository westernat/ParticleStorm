package org.mesdag.particlestorm.api;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.function.BiConsumer;

public class RegisterMolangQueriesEvent extends Event implements IModBusEvent {
    private final BiConsumer<String, ToFloatFunction<MolangInstance>> variable;

    public RegisterMolangQueriesEvent(BiConsumer<String, ToFloatFunction<MolangInstance>> variable) {
        this.variable = variable;
    }

    public void registerVariable(String name, ToFloatFunction<MolangInstance> value) {
        variable.accept(name, value);
    }
}

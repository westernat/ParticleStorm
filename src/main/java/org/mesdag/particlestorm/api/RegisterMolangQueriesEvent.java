package org.mesdag.particlestorm.api;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;

public class RegisterMolangQueriesEvent extends Event implements IModBusEvent {
    private final BiConsumer<String, ToDoubleFunction<MolangInstance>> variable;

    public RegisterMolangQueriesEvent(BiConsumer<String, ToDoubleFunction<MolangInstance>> variable) {
        this.variable = variable;
    }

    public void registerVariable(String name, ToDoubleFunction<MolangInstance> value) {
        variable.accept(name, value);
    }
}

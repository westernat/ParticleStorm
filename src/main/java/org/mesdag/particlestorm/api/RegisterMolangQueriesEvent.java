package org.mesdag.particlestorm.api;

import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;

public class RegisterMolangQueriesEvent {
    private final BiConsumer<String, ToDoubleFunction<MolangInstance>> variable;

    public RegisterMolangQueriesEvent(BiConsumer<String, ToDoubleFunction<MolangInstance>> variable) {
        this.variable = variable;
    }

    public void registerVariable(String name, ToDoubleFunction<MolangInstance> value) {
        variable.accept(name, value);
    }
}

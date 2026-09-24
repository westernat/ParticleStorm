package org.mesdag.particlestorm.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;

public class RegisterMolangQueriesEvent {
    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class, listeners -> event -> {
        for (Callback listener : listeners) {
            listener.onRegisterMolangQueries(event);
        }
    });

    @FunctionalInterface
    public interface Callback {
        void onRegisterMolangQueries(RegisterMolangQueriesEvent event);
    }

    private final BiConsumer<String, ToDoubleFunction<MolangInstance>> variable;

    public RegisterMolangQueriesEvent(BiConsumer<String, ToDoubleFunction<MolangInstance>> variable) {
        this.variable = variable;
    }

    public void registerVariable(String name, ToDoubleFunction<MolangInstance> value) {
        variable.accept(name, value);
    }
}

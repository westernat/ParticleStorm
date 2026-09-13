package org.mesdag.particlestorm.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import org.jetbrains.annotations.ApiStatus;
import org.mesdag.particlestorm.particle.EmitterPreset;

public class EmitterPresetLoadedEvent {
    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class, listeners -> event -> {
        for (Callback listener : listeners) {
            listener.onEmitterPresetLoaded(event);
        }
    });

    private final EmitterPreset preset;

    @ApiStatus.Internal
    public EmitterPresetLoadedEvent(EmitterPreset preset) {
        this.preset = preset;
    }

    public EmitterPreset getPreset() {
        return preset;
    }

    @FunctionalInterface
    public interface Callback {
        void onEmitterPresetLoaded(EmitterPresetLoadedEvent event);
    }
}

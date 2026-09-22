package org.mesdag.particlestorm.api;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.mesdag.particlestorm.particle.EmitterPreset;

/// Fired on the mod event bus after an {@link EmitterPreset} has been fully constructed and its
/// Molang expressions compiled. Listeners may attach extra data through {@link EmitterPreset#setTicket}.
public class EmitterPresetLoadedEvent extends Event implements IModBusEvent {
    private final EmitterPreset preset;

    public EmitterPresetLoadedEvent(EmitterPreset preset) {
        this.preset = preset;
    }

    public EmitterPreset getPreset() {
        return preset;
    }
}

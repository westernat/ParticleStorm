package org.mesdag.particlestorm.api;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import org.mesdag.particlestorm.particle.EmitterPreset;

public class EmitterPresetLoadedEvent extends Event implements IModBusEvent {
    private final EmitterPreset preset;

    public EmitterPresetLoadedEvent(EmitterPreset preset) {
        this.preset = preset;
    }

    public EmitterPreset getPreset() {
        return preset;
    }
}

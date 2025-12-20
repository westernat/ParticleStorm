package org.mesdag.particlestorm.api;

import net.neoforged.bus.api.Event;
import org.mesdag.particlestorm.particle.ParticlePreset;

public class ParticlePresetLoadedEvent extends Event {
    private final ParticlePreset preset;

    public ParticlePresetLoadedEvent(ParticlePreset preset) {
        this.preset = preset;
    }

    public ParticlePreset getPreset() {
        return preset;
    }
}

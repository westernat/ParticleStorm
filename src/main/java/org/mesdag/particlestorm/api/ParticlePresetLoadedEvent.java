package org.mesdag.particlestorm.api;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.particle.ParticlePreset;

public class ParticlePresetLoadedEvent extends Event implements IModBusEvent {
    private final ParticlePreset preset;

    public ParticlePresetLoadedEvent(ParticlePreset preset) {
        this.preset = preset;
    }

    public DefinedParticleEffect getEffect() {
        return preset.effect;
    }

    public ParticlePreset getPreset() {
        return preset;
    }
}

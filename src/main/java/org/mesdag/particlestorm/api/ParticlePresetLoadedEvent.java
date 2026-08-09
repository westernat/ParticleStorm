package org.mesdag.particlestorm.api;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.particle.ParticlePreset;

public class ParticlePresetLoadedEvent extends Event implements IModBusEvent {
    private final DefinedParticleEffect effect;
    private final ParticlePreset preset;

    public ParticlePresetLoadedEvent(DefinedParticleEffect effect, ParticlePreset preset) {
        this.effect = effect;
        this.preset = preset;
    }

    public DefinedParticleEffect getEffect() {
        return effect;
    }

    public ParticlePreset getPreset() {
        return preset;
    }
}

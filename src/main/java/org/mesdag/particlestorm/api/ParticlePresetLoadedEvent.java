package org.mesdag.particlestorm.api;

import net.minecraftforge.eventbus.api.Event;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.particle.ParticlePreset;

public class ParticlePresetLoadedEvent extends Event {
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

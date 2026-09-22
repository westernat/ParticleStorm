package org.mesdag.particlestorm.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import org.jetbrains.annotations.ApiStatus;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.particle.ParticlePreset;

public class ParticlePresetLoadedEvent {
    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class, listeners -> event -> {
        for (Callback listener : listeners) {
            listener.onParticlePresetLoaded(event);
        }
    });

    private final DefinedParticleEffect effect;
    private final ParticlePreset preset;

    @ApiStatus.Internal
    public ParticlePresetLoadedEvent(ParticlePreset preset) {
        this.effect = preset.effect;
        this.preset = preset;
    }

    public DefinedParticleEffect getEffect() {
        return effect;
    }

    public ParticlePreset getPreset() {
        return preset;
    }

    @FunctionalInterface
    public interface Callback {
        void onParticlePresetLoaded(ParticlePresetLoadedEvent event);
    }
}

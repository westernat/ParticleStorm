package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.List;
import java.util.Map;

/**
 * All events use the event names in the event section<p>
 * All events can be either an array or a string
 *
 * @param creationEvent   Fires when the particle is created
 * @param expirationEvent Fires when the particle expires (does not wait for particles to expire too)
 * @param timeline        A series of times, e.g. 0.0 or 1.0, that trigger the event
 */
public record ParticleLifeTimeEvents(List<String> creationEvent, List<String> expirationEvent, Map<String, List<String>> timeline) implements IParticleComponent {
    public static final Codec<ParticleLifeTimeEvents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ParticleStorm.STRING_LIST_CODEC.fieldOf("creation_event").orElseGet(List::of).forGetter(ParticleLifeTimeEvents::creationEvent),
            ParticleStorm.STRING_LIST_CODEC.fieldOf("expiration_event").orElseGet(List::of).forGetter(ParticleLifeTimeEvents::expirationEvent),
            Codec.unboundedMap(Codec.STRING, ParticleStorm.STRING_LIST_CODEC).fieldOf("timeline").orElseGet(Map::of).forGetter(ParticleLifeTimeEvents::timeline)
    ).apply(instance, ParticleLifeTimeEvents::new));

    @Override
    public Codec<ParticleLifeTimeEvents> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of();
    }
}

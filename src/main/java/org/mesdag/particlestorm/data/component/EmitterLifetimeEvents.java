package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.List;
import java.util.Map;

/**
 * Allows for lifetime events on the emitter to trigger certain events.<p>
 * All events use the event names in the event section<p>
 * All events can be an array or a string
 *
 * @param creationEvent               Fires when the emitter is created
 * @param expirationEvent             Fires when the emitter expires (does not wait for particles to expire too)
 * @param timeline                    A series of times, e.g. 0.0 or 1.0, that trigger the event.<p>
 *                                    These get fired on every loop the emitter goes through
 * @param travelDistanceEvents        S series of distances, e.g. 0.0 or 1.0, that trigger the event.<p>
 *                                    These get fired when the emitter has moved by the specified input
 * @param loopingTravelDistanceEvents A series of events that occur at set intervals.<p>
 *                                    These get fired every time the emitter has moved the specified input distance from the last time it was fired.
 */
public record EmitterLifetimeEvents(List<String> creationEvent, List<String> expirationEvent, Map<String, List<String>> timeline, Map<String, List<String>> travelDistanceEvents, List<LoopingTravelDistanceEvent> loopingTravelDistanceEvents) implements IEmitterComponent {
    public static final Codec<EmitterLifetimeEvents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ParticleStorm.STRING_LIST_CODEC.fieldOf("creation_event").orElseGet(List::of).forGetter(EmitterLifetimeEvents::creationEvent),
            ParticleStorm.STRING_LIST_CODEC.fieldOf("expiration_event").orElseGet(List::of).forGetter(EmitterLifetimeEvents::expirationEvent),
            Codec.unboundedMap(Codec.STRING, ParticleStorm.STRING_LIST_CODEC).fieldOf("timeline").orElseGet(Map::of).forGetter(EmitterLifetimeEvents::timeline),
            Codec.unboundedMap(Codec.STRING, ParticleStorm.STRING_LIST_CODEC).fieldOf("travel_distance_events").orElseGet(Map::of).forGetter(EmitterLifetimeEvents::travelDistanceEvents),
            Codec.list(LoopingTravelDistanceEvent.CODEC).fieldOf("looping_travel_distance_events").orElseGet(List::of).forGetter(EmitterLifetimeEvents::loopingTravelDistanceEvents)
    ).apply(instance, EmitterLifetimeEvents::new));

    @Override
    public Codec<EmitterLifetimeEvents> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of();
    }

    public record LoopingTravelDistanceEvent(float distance, List<String> effects) {
        public static final Codec<LoopingTravelDistanceEvent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("distance").forGetter(LoopingTravelDistanceEvent::distance),
                ParticleStorm.STRING_LIST_CODEC.fieldOf("effects").forGetter(LoopingTravelDistanceEvent::effects)
        ).apply(instance, LoopingTravelDistanceEvent::new));
    }
}

package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.datafixers.util.Pair;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.IEmitterComponent;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.data.event.EventResolver;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/// Allows for lifetime events on the emitter to trigger certain events.
///
/// All events use the event names in the event section
///
/// All events can be an array or a string
public final class EmitterLifetimeEvents implements IEmitterComponent {
    public static final Codec<EmitterLifetimeEvents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ParticleStorm.STRING_LIST_CODEC.fieldOf("creation_event").orElseGet(List::of).forGetter(events -> events.creationEvent),
            ParticleStorm.STRING_LIST_CODEC.fieldOf("expiration_event").orElseGet(List::of).forGetter(events -> events.expirationEvent),
            Codec.unboundedMap(Codec.STRING, ParticleStorm.STRING_LIST_CODEC).fieldOf("timeline").orElseGet(Map::of).forGetter(events -> events.timeline),
            Codec.unboundedMap(Codec.STRING, ParticleStorm.STRING_LIST_CODEC).fieldOf("travel_distance_events").orElseGet(Map::of).forGetter(events -> events.travelDistanceEvents),
            Codec.list(LoopingTravelDistanceEvent.CODEC).fieldOf("looping_travel_distance_events").orElseGet(List::of).forGetter(events -> events.loopingTravelDistanceEvents)
    ).apply(instance, EmitterLifetimeEvents::new));
    public final List<String> creationEvent;
    public final List<String> expirationEvent;
    public final Map<String, List<String>> timeline;
    public final Map<String, List<String>> travelDistanceEvents;
    public final List<LoopingTravelDistanceEvent> loopingTravelDistanceEvents;

    public final List<Pair<Function<Integer, Boolean>, List<String>>> sortedTimeline;
    public final List<Pair<Function<Float, Boolean>, List<String>>> sortedTravelDistance;

    /// @param creationEvent               Fires when the emitter is created
    /// @param expirationEvent             Fires when the emitter expires (does not wait for particles to expire too)
    /// @param timeline                    A series of times, e.g. 0.0 or 1.0, that trigger the event.<p>
    ///                                    These get fired on every loop the emitter goes through
    /// @param travelDistanceEvents        S series of distances, e.g. 0.0 or 1.0, that trigger the event.<p>
    ///                                    These get fired when the emitter has moved by the specified input
    /// @param loopingTravelDistanceEvents A series of events that occur at set intervals.<p>
    ///                                    These get fired every time the emitter has moved the specified input distance from the last time it was fired.
    public EmitterLifetimeEvents(List<String> creationEvent, List<String> expirationEvent, Map<String, List<String>> timeline, Map<String, List<String>> travelDistanceEvents, List<LoopingTravelDistanceEvent> loopingTravelDistanceEvents) {
        this.creationEvent = creationEvent;
        this.expirationEvent = expirationEvent;
        this.timeline = timeline;
        this.travelDistanceEvents = travelDistanceEvents;
        this.loopingTravelDistanceEvents = loopingTravelDistanceEvents;

        this.sortedTimeline = new ArrayList<>();
        timeline.entrySet().stream()
                .map(entry -> Pair.of(Float.parseFloat(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(Pair::getFirst))
                .forEachOrdered(tuple -> sortedTimeline.add(Pair.of(time -> time >= tuple.getFirst() * 20, tuple.getSecond())));
        this.sortedTravelDistance = new ArrayList<>();
        travelDistanceEvents.entrySet().stream()
                .map(entry -> Pair.of(Float.parseFloat(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(Pair::getFirst))
                .forEachOrdered(tuple -> sortedTravelDistance.add(Pair.of(dist -> dist >= tuple.getFirst(), tuple.getSecond())));
    }

    @Override
    public Codec<EmitterLifetimeEvents> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of();
    }

    @Override
    public void update(ParticleEmitter emitter) {
        for (int i = emitter.lastTimeline; i < sortedTimeline.size(); i++) {
            Pair<Function<Integer, Boolean>, List<String>> tuple = sortedTimeline.get(i);
            if (tuple.getFirst().apply(emitter.age)) {
                emitter.lastTimeline = i + 1;
                executes(emitter, tuple.getSecond());
                break;
            }
        }
        if (emitter.moveDist == emitter.moveDistO) return;
        for (int i = emitter.lastTravelDist; i < sortedTravelDistance.size(); i++) {
            Pair<Function<Float, Boolean>, List<String>> tuple = sortedTravelDistance.get(i);
            if (tuple.getFirst().apply(emitter.moveDist)) {
                emitter.lastTravelDist = i + 1;
                executes(emitter, tuple.getSecond());
                break;
            }
        }
        for (int i = 0; i < loopingTravelDistanceEvents.size(); i++) {
            LoopingTravelDistanceEvent loopingEvent = loopingTravelDistanceEvents.get(i);
            if (emitter.moveDist - emitter.cachedLooping[i] >= loopingEvent.distance) {
                emitter.cachedLooping[i] = emitter.moveDist;
                executes(emitter, loopingEvent.effects);
                break;
            }
        }
    }

    @Override
    public void apply(ParticleEmitter emitter) {
        emitter.children.removeIf(child -> {
            child.parent = null;
            child.remove();
            return true;
        });
        executes(emitter, creationEvent);
        emitter.cachedLooping = new float[loopingTravelDistanceEvents.size()];
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    public void onExpiration(ParticleEmitter emitter) {
        executes(emitter, expirationEvent);
    }

    @Override
    public String toString() {
        return "EmitterLifetimeEvents[" +
                "creationEvent=" + creationEvent + ", " +
                "expirationEvent=" + expirationEvent + ", " +
                "timeline=" + timeline + ", " +
                "travelDistanceEvents=" + travelDistanceEvents + ", " +
                "loopingTravelDistanceEvents=" + loopingTravelDistanceEvents + ']';
    }

    private static void executes(ParticleEmitter emitter, List<String> triggers) {
        for (String event : triggers) {
            for (IEventNode node : EventResolver.resolve(emitter.getPreset().events, event).values()) {
                node.execute(emitter);
            }
        }
    }

    public record LoopingTravelDistanceEvent(float distance, List<String> effects) {
        public static final Codec<LoopingTravelDistanceEvent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("distance").forGetter(LoopingTravelDistanceEvent::distance),
                ParticleStorm.STRING_LIST_CODEC.fieldOf("effects").forGetter(LoopingTravelDistanceEvent::effects)
        ).apply(instance, LoopingTravelDistanceEvent::new));
    }
}

package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.floats.FloatObjectImmutablePair;
import it.unimi.dsi.fastutil.floats.FloatObjectPair;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.IEmitterComponent;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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

    public final List<FloatObjectPair<List<String>>> sortedTimeline;
    public final List<FloatObjectPair<List<String>>> sortedTravelDistance;

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
                .map(entry -> new FloatObjectImmutablePair<>(Float.parseFloat(entry.getKey()) * 20, entry.getValue()))
                .sorted(Comparator.comparing(FloatObjectPair::leftFloat))
                .forEachOrdered(sortedTimeline::add);
        this.sortedTravelDistance = new ArrayList<>();
        travelDistanceEvents.entrySet().stream()
                .map(entry -> new FloatObjectImmutablePair<>(Float.parseFloat(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(FloatObjectPair::leftFloat))
                .forEachOrdered(sortedTravelDistance::add);
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
            FloatObjectPair<List<String>> pair = sortedTimeline.get(i);
            if (emitter.age >= pair.leftFloat()) {
                emitter.lastTimeline = i + 1;
                executes(emitter, pair.right());
                break;
            }
        }
        if (emitter.moveDist == emitter.moveDistO) return;
        for (int i = emitter.lastTravelDist; i < sortedTravelDistance.size(); i++) {
            FloatObjectPair<List<String>> pair = sortedTravelDistance.get(i);
            if (emitter.moveDist >= pair.leftFloat()) {
                emitter.lastTravelDist = i + 1;
                executes(emitter, pair.right());
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
        List<ParticleEmitter> children = emitter.getChildren(false);
        if (children != null) {
            children.removeIf(child -> {
                child.parent = null;
                child.remove();
                return true;
            });
        }
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
            for (IEventNode node : emitter.getPreset().events.get(event).values()) {
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

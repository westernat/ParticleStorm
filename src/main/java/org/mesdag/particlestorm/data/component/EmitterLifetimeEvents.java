package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Tuple;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.data.event.IEventNode;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.ParticleEmitterEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Allows for lifetime events on the emitter to trigger certain events.<p>
 * All events use the event names in the event section<p>
 * All events can be an array or a string
 */
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

    public final ArrayList<Tuple<Function<Integer, Boolean>, List<String>>> sortedTimeline;
    public final ArrayList<Tuple<Function<Float, Boolean>, List<String>>> sortedTravelDistance;

    /**
     * @param creationEvent               Fires when the emitter is created
     * @param expirationEvent             Fires when the emitter expires (does not wait for particles to expire too)
     * @param timeline                    A series of times, e.g. 0.0 or 1.0, that trigger the event.<p>
     *                                    These get fired on every loop the emitter goes through
     * @param travelDistanceEvents        S series of distances, e.g. 0.0 or 1.0, that trigger the event.<p>
     *                                    These get fired when the emitter has moved by the specified input
     * @param loopingTravelDistanceEvents A series of events that occur at set intervals.<p>
     *                                    These get fired every time the emitter has moved the specified input distance from the last time it was fired.
     */
    public EmitterLifetimeEvents(List<String> creationEvent, List<String> expirationEvent, Map<String, List<String>> timeline, Map<String, List<String>> travelDistanceEvents, List<LoopingTravelDistanceEvent> loopingTravelDistanceEvents) {
        this.creationEvent = creationEvent;
        this.expirationEvent = expirationEvent;
        this.timeline = timeline;
        this.travelDistanceEvents = travelDistanceEvents;
        this.loopingTravelDistanceEvents = loopingTravelDistanceEvents;

        this.sortedTimeline = new ArrayList<>();
        timeline.entrySet().stream()
                .map(entry -> new Tuple<>(Float.parseFloat(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(Tuple::getA))
                .forEachOrdered(tuple -> sortedTimeline.add(new Tuple<>(time -> time >= tuple.getA() * 20, tuple.getB())));
        this.sortedTravelDistance = new ArrayList<>();
        travelDistanceEvents.entrySet().stream()
                .map(entry -> new Tuple<>(Float.parseFloat(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(Tuple::getA))
                .forEachOrdered(tuple -> sortedTravelDistance.add(new Tuple<>(dist -> dist >= tuple.getA(), tuple.getB())));
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
    public void update(ParticleEmitterEntity entity) {
        for (int i = entity.lastTimeline; i < sortedTimeline.size(); i++) {
            Tuple<Function<Integer, Boolean>, List<String>> tuple = sortedTimeline.get(i);
            if (tuple.getA().apply(entity.lifetime)) {
                entity.lastTimeline = i + 1;
                Map<String, Map<String, IEventNode>> events = entity.getDetail().events;
                for (String event : tuple.getB()) {
                    events.get(event).forEach((name, node) -> node.execute(entity));
                }
                break;
            }
        }
        if (entity.moveDist == entity.moveDistO) return;
        for (int i = entity.lastTravelDist; i < sortedTravelDistance.size(); i++) {
            Tuple<Function<Float, Boolean>, List<String>> tuple = sortedTravelDistance.get(i);
            if (tuple.getA().apply(entity.moveDist)) {
                entity.lastTravelDist = i + 1;
                Map<String, Map<String, IEventNode>> events = entity.getDetail().events;
                for (String event : tuple.getB()) {
                    events.get(event).forEach((name, node) -> node.execute(entity));
                }
                break;
            }
        }
        for (int i = 0; i < loopingTravelDistanceEvents.size(); i++) {
            LoopingTravelDistanceEvent loopingEvent = loopingTravelDistanceEvents.get(i);
            if (entity.moveDist - entity.cachedLooping[i] >= loopingEvent.distance) {
                entity.cachedLooping[i] = entity.moveDist;
                Map<String, Map<String, IEventNode>> events = entity.getDetail().events;
                for (String event : loopingEvent.effects) {
                    events.get(event).forEach((name, node) -> node.execute(entity));
                }
                break;
            }
        }
    }

    @Override
    public void apply(ParticleEmitterEntity entity) {
        Map<String, Map<String, IEventNode>> events = entity.getDetail().events;
        for (String event : creationEvent) {
            events.get(event).forEach((name, node) -> node.execute(entity));
        }
        entity.cachedLooping = new float[loopingTravelDistanceEvents.size()];
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    public void onExpiration(ParticleEmitterEntity entity) {
        Map<String, Map<String, IEventNode>> events = entity.getDetail().events;
        for (String event : expirationEvent) {
            events.get(event).forEach((name, node) -> node.execute(entity));
        }
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

    public record LoopingTravelDistanceEvent(float distance, List<String> effects) {
        public static final Codec<LoopingTravelDistanceEvent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("distance").forGetter(LoopingTravelDistanceEvent::distance),
                ParticleStorm.STRING_LIST_CODEC.fieldOf("effects").forGetter(LoopingTravelDistanceEvent::effects)
        ).apply(instance, LoopingTravelDistanceEvent::new));
    }
}

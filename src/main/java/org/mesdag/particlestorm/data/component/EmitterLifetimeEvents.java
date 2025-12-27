package org.mesdag.particlestorm.data.component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Tuple;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.Deserializer;
import org.mesdag.particlestorm.api.IEmitterComponent;
import org.mesdag.particlestorm.api.IEventNode;
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
    public final List<String> creationEvent;
    public final List<String> expirationEvent;
    public final Map<String, List<String>> timeline;
    public final Map<String, List<String>> travelDistanceEvents;
    public final List<LoopingTravelDistanceEvent> loopingTravelDistanceEvents;

    public final List<Tuple<Function<Integer, Boolean>, List<String>>> sortedTimeline;
    public final List<Tuple<Function<Float, Boolean>, List<String>>> sortedTravelDistance;

    /// @param creationEvent               Fires when the emitter is created
    /// @param expirationEvent             Fires when the emitter expires (does not wait for particles to expire too)
    /// @param timeline                    A series of times, e.g. 0.0 or 1.0, that trigger the event.<p>
    ///                                                                                                          These get fired on every loop the emitter goes through
    /// @param travelDistanceEvents        S series of distances, e.g. 0.0 or 1.0, that trigger the event.<p>
    ///                                                                                                          These get fired when the emitter has moved by the specified input
    /// @param loopingTravelDistanceEvents A series of events that occur at set intervals.<p>
    ///                                                                                                          These get fired every time the emitter has moved the specified input distance from the last time it was fired.
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
    public Deserializer<EmitterLifetimeEvents> deserializer() {
        return EmitterLifetimeEvents::fromJson;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of();
    }

    @Override
    public void update(ParticleEmitter emitter) {
        for (int i = emitter.lastTimeline; i < sortedTimeline.size(); i++) {
            Tuple<Function<Integer, Boolean>, List<String>> tuple = sortedTimeline.get(i);
            if (tuple.getA().apply(emitter.lifetime)) {
                emitter.lastTimeline = i + 1;
                executes(emitter, tuple.getB());
                break;
            }
        }
        if (emitter.moveDist == emitter.moveDistO) return;
        for (int i = emitter.lastTravelDist; i < sortedTravelDistance.size(); i++) {
            Tuple<Function<Float, Boolean>, List<String>> tuple = sortedTravelDistance.get(i);
            if (tuple.getA().apply(emitter.moveDist)) {
                emitter.lastTravelDist = i + 1;
                executes(emitter, tuple.getB());
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
            for (IEventNode node : emitter.getPreset().events.get(event).values()) {
                node.execute(emitter);
            }
        }
    }

    public static EmitterLifetimeEvents fromJson(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        List<String> creationEvent = ParticleStorm.getAsStringList(object.get("creation_event"));
        List<String> expirationEvent = ParticleStorm.getAsStringList(object.get("expiration_event"));
        JsonElement timeline1 = object.get("timeline");
        ImmutableMap.Builder<String, List<String>> builder = ImmutableMap.builder();
        if (timeline1 != null) {
            for (Map.Entry<String, JsonElement> entry : timeline1.getAsJsonObject().entrySet()) {
                builder.put(entry.getKey(), ParticleStorm.getAsStringList(entry.getValue()));
            }
        }
        JsonElement travelDistanceEvents1 = object.get("travel_distance_events");
        ImmutableMap.Builder<String, List<String>> builder1 = ImmutableMap.builder();
        if (travelDistanceEvents1 != null) {
            for (Map.Entry<String, JsonElement> entry : travelDistanceEvents1.getAsJsonObject().entrySet()) {
                builder1.put(entry.getKey(), ParticleStorm.getAsStringList(entry.getValue()));
            }
        }
        ImmutableList.Builder<LoopingTravelDistanceEvent> builder2 = ImmutableList.builder();
        JsonElement loopingTravelDistanceEvents1 = object.get("looping_travel_distance_events");
        if (loopingTravelDistanceEvents1 != null) {
            for (JsonElement jsonElement : loopingTravelDistanceEvents1.getAsJsonArray()) {
                builder2.add(LoopingTravelDistanceEvent.fromJson(jsonElement));
            }
        }
        return new EmitterLifetimeEvents(creationEvent, expirationEvent, builder.build(), builder1.build(), builder2.build());
    }

    public record LoopingTravelDistanceEvent(float distance, List<String> effects) {
        public static LoopingTravelDistanceEvent fromJson(JsonElement element) {
            JsonObject object = element.getAsJsonObject();
            float distance1 = GsonHelper.getAsFloat(object, "distance");
            List<String> effects1 = ParticleStorm.getAsStringList(GsonHelper.getNonNull(object, "effects"));
            return new LoopingTravelDistanceEvent(distance1, effects1);
        }
    }
}

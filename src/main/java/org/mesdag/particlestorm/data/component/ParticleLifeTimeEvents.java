package org.mesdag.particlestorm.data.component;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.Deserializer;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/// All events use the event names in the event section
///
/// All events can be either an array or a string
public final class ParticleLifeTimeEvents implements IParticleComponent {
    public static final ResourceLocation ID = new ResourceLocation("particle_lifetime_events");
    public final List<String> creationEvent;
    public final List<String> expirationEvent;
    public final Map<String, List<String>> timeline;

    public final List<Tuple<Function<Integer, Boolean>, List<String>>> sortedTimeline;

    /// @param creationEvent   Fires when the particle is created
    /// @param expirationEvent Fires when the particle expires (does not wait for particles to expire too)
    /// @param timeline        A series of times, e.g. 0.0 or 1.0, that trigger the event
    public ParticleLifeTimeEvents(List<String> creationEvent, List<String> expirationEvent, Map<String, List<String>> timeline) {
        this.creationEvent = creationEvent;
        this.expirationEvent = expirationEvent;
        this.timeline = timeline;

        this.sortedTimeline = new ArrayList<>();
        timeline.entrySet().stream()
                .map(entry -> new Tuple<>(Float.parseFloat(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(Tuple::getA))
                .forEachOrdered(tuple -> sortedTimeline.add(new Tuple<>(time -> time >= tuple.getA() * 20, tuple.getB())));
    }

    @Override
    public Deserializer<ParticleLifeTimeEvents> deserializer() {
        return ParticleLifeTimeEvents::fromJson;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of();
    }

    @Override
    public void update(IMolangParticleInstance instance) {
        for (int i = instance.getLastTimeline(); i < sortedTimeline.size(); i++) {
            Tuple<Function<Integer, Boolean>, List<String>> tuple = sortedTimeline.get(i);
            if (tuple.getA().apply(instance.self().getLifetime())) {
                instance.setLastTimeline(i + 1);
                executes(instance, tuple.getB());
                break;
            }
        }
    }

    @Override
    public void apply(IMolangParticleInstance instance) {
        executes(instance, creationEvent);
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    public void onExpiration(IMolangParticleInstance instance) {
        executes(instance, expirationEvent);
    }

    @Override
    public String toString() {
        return "ParticleLifeTimeEvents[" +
                "creationEvent=" + creationEvent + ", " +
                "expirationEvent=" + expirationEvent + ", " +
                "timeline=" + timeline + ']';
    }

    private static void executes(IMolangParticleInstance instance, List<String> triggers) {
        for (String event : triggers) {
            for (IEventNode node : instance.getPreset().effect.events.get(event).values()) {
                node.execute(instance);
            }
        }
    }

    public static ParticleLifeTimeEvents fromJson(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        List<String> creationEvent1 = ParticleStorm.getAsStringList(object.get("creation_event"));
        List<String> expiration_event = ParticleStorm.getAsStringList(object.get("expiration_event"));
        JsonElement timeline1 = object.get("timeline");
        ImmutableMap.Builder<String, List<String>> builder = ImmutableMap.builder();
        if (timeline1 != null) {
            for (Map.Entry<String, JsonElement> entry : timeline1.getAsJsonObject().entrySet()) {
                builder.put(entry.getKey(), ParticleStorm.getAsStringList(entry.getValue()));
            }
        }
        return new ParticleLifeTimeEvents(creationEvent1, expiration_event, builder.build());
    }
}

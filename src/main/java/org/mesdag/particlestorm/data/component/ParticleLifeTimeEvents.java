package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.data.event.IEventNode;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * All events use the event names in the event section<p>
 * All events can be either an array or a string
 */
public final class ParticleLifeTimeEvents implements IParticleComponent {
    public static final ResourceLocation ID = ResourceLocation.withDefaultNamespace("particle_lifetime_events");
    public static final Codec<ParticleLifeTimeEvents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ParticleStorm.STRING_LIST_CODEC.fieldOf("creation_event").orElseGet(List::of).forGetter(events -> events.creationEvent),
            ParticleStorm.STRING_LIST_CODEC.fieldOf("expiration_event").orElseGet(List::of).forGetter(events -> events.expirationEvent),
            Codec.unboundedMap(Codec.STRING, ParticleStorm.STRING_LIST_CODEC).fieldOf("timeline").orElseGet(Map::of).forGetter(events -> events.timeline)
    ).apply(instance, ParticleLifeTimeEvents::new));
    public final List<String> creationEvent;
    public final List<String> expirationEvent;
    public final Map<String, List<String>> timeline;

    public final ArrayList<Tuple<Function<Integer, Boolean>, List<String>>> sortedTimeline;

    /**
     * @param creationEvent   Fires when the particle is created
     * @param expirationEvent Fires when the particle expires (does not wait for particles to expire too)
     * @param timeline        A series of times, e.g. 0.0 or 1.0, that trigger the event
     */
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
    public Codec<ParticleLifeTimeEvents> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of();
    }

    @Override
    public void update(MolangParticleInstance instance) {
        for (int i = instance.lastTimeline; i < sortedTimeline.size(); i++) {
            Tuple<Function<Integer, Boolean>, List<String>> tuple = sortedTimeline.get(i);
            if (tuple.getA().apply(instance.getLifetime())) {
                instance.lastTimeline = i + 1;
                Map<String, Map<String, IEventNode>> events = instance.detail.effect.events;
                for (String event : tuple.getB()) {
                    events.get(event).forEach((name, node) -> node.execute(instance));
                }
                break;
            }
        }
    }

    @Override
    public void apply(MolangParticleInstance instance) {
        Map<String, Map<String, IEventNode>> events = instance.detail.effect.events;
        for (String event : creationEvent) {
            events.get(event).forEach((name, node) -> node.execute(instance));
        }
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    public void onExpiration(MolangParticleInstance instance) {
        Map<String, Map<String, IEventNode>> events = instance.detail.effect.events;
        for (String event : expirationEvent) {
            events.get(event).forEach((name, node) -> node.execute(instance));
        }
    }

    @Override
    public String toString() {
        return "ParticleLifeTimeEvents[" +
                "creationEvent=" + creationEvent + ", " +
                "expirationEvent=" + expirationEvent + ", " +
                "timeline=" + timeline + ']';
    }

}

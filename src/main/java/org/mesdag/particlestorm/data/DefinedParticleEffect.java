package org.mesdag.particlestorm.data;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.mesdag.particlestorm.api.IComponent;
import org.mesdag.particlestorm.api.IEmitterComponent;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.curve.ParticleCurve;
import org.mesdag.particlestorm.data.description.ParticleDescription;

import java.util.*;

public class DefinedParticleEffect {
    public final ParticleDescription description;
    public final Map<String, ParticleCurve> curves;
    public final Map<ResourceLocation, IComponent> components;
    public final Map<String, Map<String, IEventNode>> events;

    public final List<IComponent> orderedComponents;
    public final List<IParticleComponent> orderedParticleComponents;
    public final List<IParticleComponent> orderedParticleComponentsWhichRequireUpdate;
    public final List<IParticleComponent> orderedParticleEarlyComponents;
    public final List<IEmitterComponent> orderedEmitterComponents;

    public DefinedParticleEffect(ParticleDescription description, Map<String, ParticleCurve> curves, Map<ResourceLocation, IComponent> components, Map<String, Map<String, IEventNode>> events) {
        this.description = description;
        this.curves = curves;
        this.components = components;
        this.events = events;

        this.orderedComponents = new ArrayList<>();
        this.orderedParticleComponents = new ArrayList<>();
        this.orderedParticleComponentsWhichRequireUpdate = new ArrayList<>();
        this.orderedParticleEarlyComponents = new ArrayList<>();
        this.orderedEmitterComponents = new ArrayList<>();
        components.values().stream().sorted(Comparator.comparing(IComponent::order)).forEachOrdered(orderedComponents::add);
        for (IComponent component : orderedComponents) {
            if (component instanceof IParticleComponent particleComponent) {
                if (component.order() < 0) {
                    orderedParticleEarlyComponents.add(particleComponent);
                    continue;
                }
                orderedParticleComponents.add(particleComponent);
                if (particleComponent.requireUpdate()) {
                    orderedParticleComponentsWhichRequireUpdate.add(particleComponent);
                }
            } else if (component instanceof IEmitterComponent emitterComponent) {
                orderedEmitterComponents.add(emitterComponent);
            }
        }
    }

    public static DefinedParticleEffect fromJson(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        ParticleDescription description = ParticleDescription.fromJson(GsonHelper.getNonNull(object, "description"));
        ImmutableMap.Builder<String, ParticleCurve> curves = ImmutableMap.builder();
        JsonElement jsonCurves = object.get("curves");
        if (jsonCurves != null) {
            for (Map.Entry<String, JsonElement> entry : jsonCurves.getAsJsonObject().entrySet()) {
                curves.put(entry.getKey(), ParticleCurve.fromJson(entry.getValue()));
            }
        }
        ImmutableMap.Builder<ResourceLocation, IComponent> components = ImmutableMap.builder();
        JsonObject components1 = GsonHelper.getAsJsonObject(object, "components");
        for (Map.Entry<String, JsonElement> entry : components1.entrySet()) {
            ResourceLocation key = new ResourceLocation(entry.getKey());
            components.put(key, Objects.requireNonNull(IComponent.COMPONENTS.get(key)).fromJson(entry.getValue()));
        }
        ImmutableMap.Builder<String, Map<String, IEventNode>> events = ImmutableMap.builder();
        JsonElement events1 = object.get("events");
        if (events1 != null) {
            for (Map.Entry<String, JsonElement> entry : events1.getAsJsonObject().entrySet()) {
                ImmutableMap.Builder<String, IEventNode> nodes = ImmutableMap.builder();
                for (Map.Entry<String, JsonElement> elementEntry : entry.getValue().getAsJsonObject().entrySet()) {
                    String key = elementEntry.getKey();
                    nodes.put(key, IEventNode.getDeserializer(key).fromJson(elementEntry.getValue()));
                }
                events.put(entry.getKey(), nodes.build());
            }
        }
        return new DefinedParticleEffect(description, curves.build(), components.build(), events.build());
    }
}
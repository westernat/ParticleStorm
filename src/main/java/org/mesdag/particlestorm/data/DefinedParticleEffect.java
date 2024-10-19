package org.mesdag.particlestorm.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.data.component.IComponent;
import org.mesdag.particlestorm.data.component.IEmitterComponent;
import org.mesdag.particlestorm.data.component.IParticleComponent;
import org.mesdag.particlestorm.data.curve.ParticleCurve;
import org.mesdag.particlestorm.data.description.ParticleDescription;
import org.mesdag.particlestorm.data.event.IEventNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class DefinedParticleEffect {
    public static final Codec<DefinedParticleEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ParticleDescription.CODEC.fieldOf("description").forGetter(effect -> effect.description),
            Codec.unboundedMap(Codec.STRING, ParticleCurve.CODEC).fieldOf("curves").orElseGet(Map::of).forGetter(effect -> effect.curves),
            Codec.dispatchedMap(ResourceLocation.CODEC, IComponent.COMPONENTS::get).fieldOf("components").forGetter(effect -> effect.components),
            Codec.unboundedMap(Codec.STRING, IEventNode.CODEC).fieldOf("events").orElseGet(Map::of).forGetter(effect -> effect.events)
    ).apply(instance, DefinedParticleEffect::new));
    public final ParticleDescription description;
    public final Map<String, ParticleCurve> curves;
    public final Map<ResourceLocation, IComponent> components;
    public final Map<String, Map<String, IEventNode>> events;

    public final List<IComponent> orderedComponents;
    public final List<IParticleComponent> orderedParticleComponents;
    public final List<IParticleComponent> orderedParticleComponentsWhichRequireUpdate;
    public final List<IEmitterComponent> orderedEmitterComponents;

    public DefinedParticleEffect(ParticleDescription description, Map<String, ParticleCurve> curves, Map<ResourceLocation, IComponent> components, Map<String, Map<String, IEventNode>> events) {
        this.description = description;
        this.curves = curves;
        this.components = components;
        this.events = events;

        this.orderedComponents = new ArrayList<>();
        this.orderedParticleComponents = new ArrayList<>();
        this.orderedParticleComponentsWhichRequireUpdate = new ArrayList<>();
        this.orderedEmitterComponents = new ArrayList<>();
        components.values().stream().sorted(Comparator.comparing(IComponent::order)).forEachOrdered(orderedComponents::add);
        for (IComponent component : orderedComponents) {
            if (component instanceof IParticleComponent particleComponent) {
                orderedParticleComponents.add(particleComponent);
                if (particleComponent.requireUpdate()) {
                    orderedParticleComponentsWhichRequireUpdate.add(particleComponent);
                }
            } else if (component instanceof IEmitterComponent emitterComponent) {
                orderedEmitterComponents.add(emitterComponent);
            }
        }
    }
}

package org.mesdag.particlestorm.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.data.component.IComponent;
import org.mesdag.particlestorm.data.curve.ParticleCurve;
import org.mesdag.particlestorm.data.description.ParticleDescription;

import java.util.Map;

public class ParticleEffect {
    public static final Codec<ParticleEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ParticleDescription.CODEC.fieldOf("description").forGetter(ParticleEffect::getDescription),
            Codec.unboundedMap(Codec.STRING, ParticleCurve.CODEC).fieldOf("curves").orElseGet(Map::of).forGetter(ParticleEffect::getCurves),
            Codec.dispatchedMap(ResourceLocation.CODEC, IComponent.COMPONENTS::get).fieldOf("components").forGetter(ParticleEffect::getComponents)
    ).apply(instance, ParticleEffect::new));
    protected final ParticleDescription description;
    protected final Map<String, ParticleCurve> curves;
    protected final Map<ResourceLocation, IComponent> components;

    public ParticleEffect(ParticleDescription description, Map<String, ParticleCurve> curves, Map<ResourceLocation, IComponent> components) {
        this.description = description;
        this.curves = curves;
        this.components = components;
    }

    public ParticleDescription getDescription() {
        return description;
    }

    public Map<String, ParticleCurve> getCurves() {
        return curves;
    }

    public Map<ResourceLocation, IComponent> getComponents() {
        return components;
    }
}

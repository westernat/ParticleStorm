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
            ParticleDescription.CODEC.fieldOf("description").forGetter(effect -> effect.description),
            Codec.unboundedMap(Codec.STRING, ParticleCurve.CODEC).fieldOf("curves").orElseGet(Map::of).forGetter(effect -> effect.curves),
            Codec.dispatchedMap(ResourceLocation.CODEC, IComponent.COMPONENTS::get).fieldOf("components").forGetter(effect -> effect.components)
    ).apply(instance, ParticleEffect::new));
    public final ParticleDescription description;
    public final Map<String, ParticleCurve> curves;
    public final Map<ResourceLocation, IComponent> components;

    public ParticleEffect(ParticleDescription description, Map<String, ParticleCurve> curves, Map<ResourceLocation, IComponent> components) {
        this.description = description;
        this.curves = curves;
        this.components = components;
    }
}

package org.mesdag.particlestorm.data.description;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.ParticleStorm;

public record ParticleDescription(ResourceLocation identifier, DescriptionParameters parameters, ParticleType<?> type) {
    public static final Codec<ParticleDescription> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("identifier").forGetter(ParticleDescription::identifier),
            DescriptionParameters.CODEC.optionalFieldOf("basic_render_parameters", DescriptionParameters.EMPTY).forGetter(ParticleDescription::parameters),
            BuiltInRegistries.PARTICLE_TYPE.byNameCodec().fieldOf("type").orElseGet(ParticleStorm.MOLANG).forGetter(ParticleDescription::type)
    ).apply(instance, ParticleDescription::new));

    public ParticleDescription(ResourceLocation identifier, DescriptionParameters parameters) {
        this(identifier, parameters, ParticleStorm.MOLANG.get());
    }
}

package org.mesdag.particlestorm.data.description;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record ParticleDescription(ResourceLocation identifier, DescriptionParameters parameters) {
    public static final Codec<ParticleDescription> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("identifier").forGetter(ParticleDescription::identifier),
            DescriptionParameters.CODEC.fieldOf("basic_render_parameters").forGetter(ParticleDescription::parameters)
    ).apply(instance, ParticleDescription::new));
}

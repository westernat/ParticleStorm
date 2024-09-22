package org.mesdag.particlestorm.data.description;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record DescriptionParameters(DescriptionMaterial material, ResourceLocation texture) {
    public static final Codec<DescriptionParameters> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DescriptionMaterial.CODEC.fieldOf("material").orElse(DescriptionMaterial.PARTICLE_SHEET_OPAQUE).forGetter(DescriptionParameters::material),
            ResourceLocation.CODEC.fieldOf("texture").forGetter(DescriptionParameters::texture)
    ).apply(instance, DescriptionParameters::new));
}

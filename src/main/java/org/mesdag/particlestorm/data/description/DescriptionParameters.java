package org.mesdag.particlestorm.data.description;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.ParticleStorm;

public record DescriptionParameters(DescriptionMaterial material, ResourceLocation texture) {
    public static final ResourceLocation MISSING_TEXTURE = ParticleStorm.asResource("missing");
    public static final DescriptionParameters EMPTY = new DescriptionParameters(DescriptionMaterial.CUSTOM, MISSING_TEXTURE);
    public static final Codec<DescriptionParameters> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DescriptionMaterial.CODEC.optionalFieldOf("material", DescriptionMaterial.CUSTOM).forGetter(DescriptionParameters::material),
            ResourceLocation.CODEC.optionalFieldOf("texture", MISSING_TEXTURE).forGetter(DescriptionParameters::texture)
    ).apply(instance, DescriptionParameters::new));
}

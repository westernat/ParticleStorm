package org.mesdag.particlestorm.data.description;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.ParticleStorm;

import java.util.Objects;

public record DescriptionParameters(DescriptionMaterial material, ResourceLocation texture) {
    public static final ResourceLocation MISSING_TEXTURE = ParticleStorm.asResource("missing");
    public static final DescriptionParameters EMPTY = new DescriptionParameters(DescriptionMaterial.CUSTOM, MISSING_TEXTURE);
    public static final Codec<DescriptionParameters> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DescriptionMaterial.CODEC.lenientOptionalFieldOf("material", DescriptionMaterial.CUSTOM).forGetter(DescriptionParameters::material),
            ResourceLocation.CODEC.lenientOptionalFieldOf("texture", MISSING_TEXTURE).forGetter(DescriptionParameters::texture)
    ).apply(instance, DescriptionParameters::new));

    @Override
    public boolean equals(Object o) {
        return o == this || (o instanceof DescriptionParameters(DescriptionMaterial material1, ResourceLocation texture1) && Objects.equals(texture, texture1) && Objects.equals(material, material1));
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(material);
        result = 31 * result + Objects.hashCode(texture);
        return result;
    }
}

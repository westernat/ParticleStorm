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
            DescriptionMaterial.CODEC.optionalFieldOf("material", DescriptionMaterial.CUSTOM).forGetter(DescriptionParameters::material),
            ResourceLocation.CODEC.optionalFieldOf("texture", MISSING_TEXTURE).forGetter(DescriptionParameters::texture)
    ).apply(instance, DescriptionParameters::new));

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof DescriptionParameters other)) return false;
        return Objects.equals(texture, other.texture) && Objects.equals(material, other.material);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(material);
        result = 31 * result + Objects.hashCode(texture);
        return result;
    }
}

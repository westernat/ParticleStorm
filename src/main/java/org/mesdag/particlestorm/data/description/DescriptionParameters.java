package org.mesdag.particlestorm.data.description;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.ParticleStorm;

public class DescriptionParameters {
    public static final ResourceLocation MISSING_TEXTURE = ParticleStorm.asResource("missing");
    public static final DescriptionParameters EMPTY = new DescriptionParameters(DescriptionMaterial.CUSTOM, MISSING_TEXTURE);
    public static final Codec<DescriptionParameters> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DescriptionMaterial.CODEC.lenientOptionalFieldOf("material", DescriptionMaterial.CUSTOM).orElse(DescriptionMaterial.PARTICLE_SHEET_TRANSLUCENT).forGetter(DescriptionParameters::material),
            ResourceLocation.CODEC.lenientOptionalFieldOf("texture", MISSING_TEXTURE).forGetter(DescriptionParameters::texture)
    ).apply(instance, DescriptionParameters::new));
    private final DescriptionMaterial material;
    private final ResourceLocation texture;
    private int index = -1;

    public DescriptionParameters(DescriptionMaterial material, ResourceLocation texture) {
        this.material = material;
        this.texture = texture;
    }

    public DescriptionMaterial material() {
        return material;
    }

    public ResourceLocation texture() {
        return texture;
    }

    public ResourceLocation bindTexture(int index) {
        this.index = index;
        return texture;
    }

    public int getTextureIndex() {
        return index;
    }
}

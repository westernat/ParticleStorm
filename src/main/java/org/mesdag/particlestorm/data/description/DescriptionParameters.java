package org.mesdag.particlestorm.data.description;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.ParticleStorm;

public class DescriptionParameters {
    public static final ResourceLocation MISSING_TEXTURE = ParticleStorm.asResource("missing");
    public static final DescriptionParameters EMPTY = new DescriptionParameters(DescriptionMaterial.CUSTOM, MISSING_TEXTURE);
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

    public static DescriptionParameters fromJson(@Nullable JsonElement element) {
        if (element == null) return EMPTY;
        JsonObject object = element.getAsJsonObject();
        DescriptionMaterial material = DescriptionMaterial.fromJson(object.get("material"));
        JsonElement jsonTexture = object.get("texture");
        ResourceLocation texture = jsonTexture == null ? MISSING_TEXTURE : new ResourceLocation(jsonTexture.getAsString());
        return new DescriptionParameters(material, texture);
    }
}

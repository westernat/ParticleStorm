package org.mesdag.particlestorm.data.description;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.mesdag.particlestorm.ParticleStorm;

public record ParticleDescription(ResourceLocation identifier, DescriptionParameters parameters, ParticleType<?> type) {
    public ParticleDescription(ResourceLocation identifier, DescriptionParameters parameters) {
        this(identifier, parameters, ParticleStorm.MOLANG.get());
    }

    public static ParticleDescription fromJson(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        ResourceLocation identifier = new ResourceLocation(GsonHelper.getAsString(object, "identifier"));
        DescriptionParameters parameters = DescriptionParameters.fromJson(object.get("basic_render_parameters"));
        JsonElement jsonType = object.get("type");
        ParticleType<?> type = jsonType == null ? ParticleStorm.MOLANG.get() : ForgeRegistries.PARTICLE_TYPES.getDelegateOrThrow(new ResourceLocation(jsonType.getAsString())).value();
        return new ParticleDescription(identifier, parameters, type);
    }
}

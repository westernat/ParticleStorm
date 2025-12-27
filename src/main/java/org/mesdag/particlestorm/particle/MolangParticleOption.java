package org.mesdag.particlestorm.particle;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.mesdag.particlestorm.ParticleStorm;

public record MolangParticleOption(ResourceLocation id) implements ParticleOptions {
    public static final Codec<MolangParticleOption> CODEC = ResourceLocation.CODEC.xmap(MolangParticleOption::new, MolangParticleOption::id);

    @Override
    public ParticleType<MolangParticleOption> getType() {
        return ParticleStorm.MOLANG.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(id);
    }

    @Override
    public String writeToString() {
        return ForgeRegistries.PARTICLE_TYPES.getKey(getType()) + " " + id;
    }
}

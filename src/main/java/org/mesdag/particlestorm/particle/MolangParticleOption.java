package org.mesdag.particlestorm.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.ParticleStorm;

public record MolangParticleOption(ResourceLocation id) implements ParticleOptions {
    public static final Codec<MolangParticleOption> CODEC = ResourceLocation.CODEC.xmap(MolangParticleOption::new, MolangParticleOption::id);
    public static final ParticleOptions.Deserializer<MolangParticleOption> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public MolangParticleOption fromCommand(ParticleType<MolangParticleOption> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            ResourceLocation id = ResourceLocation.read(reader);
            return new MolangParticleOption(id);
        }

        @Override
        public MolangParticleOption fromNetwork(ParticleType<MolangParticleOption> type, FriendlyByteBuf buf) {
            return new MolangParticleOption(buf.readResourceLocation());
        }
    };

    @Override
    public ParticleType<MolangParticleOption> getType() {
        return ParticleStorm.MOLANG.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
    }

    @Override
    public String writeToString() {
        return BuiltInRegistries.PARTICLE_TYPE.getKey(getType()) + " " + id;
    }
}

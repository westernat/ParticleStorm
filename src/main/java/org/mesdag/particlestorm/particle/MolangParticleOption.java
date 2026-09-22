package org.mesdag.particlestorm.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.ParticleStorm;

public record MolangParticleOption(ResourceLocation id) implements ParticleOptions {
    public static final Codec<MolangParticleOption> CODEC = ResourceLocation.CODEC.fieldOf("id").xmap(MolangParticleOption::new, MolangParticleOption::id).codec();
    public static final Deserializer<MolangParticleOption> DESERIALIZER = new Deserializer<>() {
        @Override
        public MolangParticleOption fromCommand(ParticleType<MolangParticleOption> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            return new MolangParticleOption(ResourceLocation.read(reader));
        }

        @Override
        public MolangParticleOption fromNetwork(ParticleType<MolangParticleOption> type, FriendlyByteBuf buf) {
            return new MolangParticleOption(buf.readResourceLocation());
        }
    };

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
    }

    @Override
    public String writeToString() {
        return BuiltInRegistries.PARTICLE_TYPE.getKey(getType()) + " " + id;
    }

    @Override
    public ParticleType<MolangParticleOption> getType() {
        return ParticleStorm.MOLANG;
    }

    public ResourceLocation getId() {
        return id;
    }

    public ParticlePreset getPreset() {
        return PSGameClient.LOADER.id2Particle().get(id);
    }
}

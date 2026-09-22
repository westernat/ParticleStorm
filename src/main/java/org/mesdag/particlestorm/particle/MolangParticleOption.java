package org.mesdag.particlestorm.particle;

import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.ParticleStorm;

public record MolangParticleOption(Identifier id) implements ParticleOptions {
    public static final MapCodec<MolangParticleOption> CODEC = Identifier.CODEC.fieldOf("id").xmap(MolangParticleOption::new, MolangParticleOption::id);
    public static final StreamCodec<ByteBuf, MolangParticleOption> STREAM_CODEC = Identifier.STREAM_CODEC.map(MolangParticleOption::new, MolangParticleOption::id);

    @Override
    public ParticleType<MolangParticleOption> getType() {
        return ParticleStorm.MOLANG;
    }

    public Identifier getId() {
        return id;
    }

    public ParticlePreset getPreset() {
        return PSGameClient.LOADER.id2Particle().get(id);
    }
}

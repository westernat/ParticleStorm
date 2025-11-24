package org.mesdag.particlestorm.particle;

import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.ParticleStorm;

public record MolangParticleOption(ResourceLocation id) implements ParticleOptions {
    public static final MapCodec<MolangParticleOption> CODEC = ResourceLocation.CODEC.fieldOf("id").xmap(MolangParticleOption::new, MolangParticleOption::id);
    public static final StreamCodec<ByteBuf, MolangParticleOption> STREAM_CODEC = ResourceLocation.STREAM_CODEC.map(MolangParticleOption::new, MolangParticleOption::id);

    @Override
    public ParticleType<MolangParticleOption> getType() {
        return ParticleStorm.MOLANG.get();
    }
}

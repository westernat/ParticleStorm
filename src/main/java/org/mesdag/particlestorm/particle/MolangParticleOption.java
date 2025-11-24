package org.mesdag.particlestorm.particle;

import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.ParticleStorm;

import java.util.function.Supplier;

public class MolangParticleOption implements ParticleOptions {
    public static final MapCodec<MolangParticleOption> CODEC = ResourceLocation.CODEC.fieldOf("id").xmap(MolangParticleOption::new, MolangParticleOption::getId);
    public static final StreamCodec<ByteBuf, MolangParticleOption> STREAM_CODEC = ResourceLocation.STREAM_CODEC.map(MolangParticleOption::new, MolangParticleOption::getId);
    private final Supplier<ParticleType<MolangParticleOption>> type;
    private final ResourceLocation id;

    private MolangParticleOption(Supplier<ParticleType<MolangParticleOption>> type, ResourceLocation id) {
        this.type = type;
        this.id = id;
    }

    public MolangParticleOption(ResourceLocation id) {
        this(ParticleStorm.MOLANG, id);
    }

    public ResourceLocation getId() {
        return id;
    }

    @Override
    public ParticleType<MolangParticleOption> getType() {
        return type.get();
    }
}

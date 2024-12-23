package org.mesdag.particlestorm.particle;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.mesdag.particlestorm.ParticleStorm;


public class MolangParticleOption implements ParticleOptions {
    private final Supplier<ParticleType<MolangParticleOption>> type;
    private final ResourceLocation id;

    private MolangParticleOption(Supplier<ParticleType<MolangParticleOption>> type, ResourceLocation id) {
        this.type = Suppliers.memoize(type);
        this.id = id;
    }

    public MolangParticleOption(ResourceLocation id) {
        this(ParticleStorm.MOLANG::get, id);
    }

    public ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull ParticleType<?> getType() {
        return type.get();
    }

    public static MapCodec<MolangParticleOption> codec(ParticleType<MolangParticleOption> type) {
        return ResourceLocation.CODEC.xmap(
                id -> new MolangParticleOption(() -> type, id),
                option -> option.id
        ).fieldOf("id");
    }

    public static StreamCodec<? super ByteBuf, MolangParticleOption> streamCodec(ParticleType<MolangParticleOption> type) {
        return ResourceLocation.STREAM_CODEC.map(id -> new MolangParticleOption(() -> type, id), option -> option.id);
    }
}

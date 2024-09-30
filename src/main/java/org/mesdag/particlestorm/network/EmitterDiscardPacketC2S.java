package org.mesdag.particlestorm.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.particle.ParticleEmitterEntity;

public record EmitterDiscardPacketC2S(int id) implements CustomPacketPayload {
    public static final Type<EmitterDiscardPacketC2S> TYPE = new Type<>(ParticleStorm.asResource("emitter_discard"));

    public static final StreamCodec<ByteBuf, EmitterDiscardPacketC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, p -> p.id,
            EmitterDiscardPacketC2S::new
    );

    @Override
    public @NotNull Type<EmitterDiscardPacketC2S> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();
            if (!level.isClientSide && level.getEntity(id) instanceof ParticleEmitterEntity entity) {
                entity.discard();
            }
        }).exceptionally(e -> {
            context.disconnect(Component.translatable("neoforge.network.invalid_flow", e.getMessage()));
            return null;
        });
    }
}

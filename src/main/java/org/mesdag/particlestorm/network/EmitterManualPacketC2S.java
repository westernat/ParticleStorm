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

public record EmitterManualPacketC2S(int id, int count) implements CustomPacketPayload {
    public static final Type<EmitterManualPacketC2S> TYPE = new Type<>(ParticleStorm.asResource("emitter_manual"));

    public static final StreamCodec<ByteBuf, EmitterManualPacketC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, p -> p.id,
            ByteBufCodecs.INT, p -> p.count,
            EmitterManualPacketC2S::new
    );

    @Override
    public @NotNull Type<EmitterManualPacketC2S> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();
            if (!level.isClientSide && level.getEntity(id) instanceof ParticleEmitterEntity entity) {
                if (entity.manualData != null) {
                    entity.manualData.doSendParticle(count);
                }
                entity.discard();
            }
        }).exceptionally(e -> {
            context.disconnect(Component.translatable("neoforge.network.invalid_flow", e.getMessage()));
            return null;
        });
    }
}

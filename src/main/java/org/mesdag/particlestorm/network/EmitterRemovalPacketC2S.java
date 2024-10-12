package org.mesdag.particlestorm.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.mesdag.particlestorm.GameClient;
import org.mesdag.particlestorm.ParticleStorm;

public record EmitterRemovalPacketC2S(int id) implements CustomPacketPayload {
    public static final Type<EmitterRemovalPacketC2S> TYPE = new Type<>(ParticleStorm.asResource("emitter_removal"));

    public static final StreamCodec<ByteBuf, EmitterRemovalPacketC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, p -> p.id,
            EmitterRemovalPacketC2S::new
    );

    @Override
    public @NotNull Type<EmitterRemovalPacketC2S> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().isLocalPlayer()) {
                GameClient.LOADER.removeEmitter(id);
            }
        }).exceptionally(e -> {
            context.disconnect(Component.translatable("neoforge.network.invalid_flow", e.getMessage()));
            return null;
        });
    }
}

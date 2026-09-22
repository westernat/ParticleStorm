package org.mesdag.particlestorm.network;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.particle.ParticleEmitter;

public record EmitterRemovalPacket(int id) implements CustomPacketPayload {
    public static final Type<EmitterRemovalPacket> TYPE = new Type<>(ParticleStorm.asResource("emitter_removal"));

    public static final StreamCodec<ByteBuf, EmitterRemovalPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, p -> p.id,
            EmitterRemovalPacket::new
    );

    @Override
    public Type<EmitterRemovalPacket> type() {
        return TYPE;
    }

    public static void handleClient(EmitterRemovalPacket payload, ClientPlayNetworking.Context context) {
        Player player = context.player();
        ParticleEmitter emitter = PSGameClient.LOADER.removeEmitter(payload.id, false);
        if (emitter == null) {
            player.sendSystemMessage(Component.translatable("commands.particlestorm.not_found", payload.id));
        } else {
            player.sendSystemMessage(Component.translatable("commands.particlestorm.remove", emitter.particleId == null ? payload.id : emitter.particleId.toString()));
        }
    }

    public static void handleServer(EmitterRemovalPacket payload, ServerPlayNetworking.Context context) {
        EmitterSynchronizePacket.getEmitterData(context.player(), false).remove(Integer.toString(payload.id));
    }

    public static void sendToServer(int id) {
        if (ClientPlayNetworking.canSend(TYPE)) {
            ClientPlayNetworking.send(new EmitterRemovalPacket(id));
        }
    }

    public static void sendToClient(ServerPlayer player, int id) {
        EmitterSynchronizePacket.getEmitterData(player, false).remove(Integer.toString(id));
        ServerPlayNetworking.send(player, new EmitterRemovalPacket(id));
    }
}

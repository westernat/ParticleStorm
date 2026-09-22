package org.mesdag.particlestorm.network;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.particle.ParticleEmitter;

public record EmitterAttachPacketS2C(int particleId, int entityId) implements CustomPacketPayload {
    public static final Type<EmitterAttachPacketS2C> TYPE = new Type<>(ParticleStorm.asResource("emitter_attach"));
    public static final StreamCodec<ByteBuf, EmitterAttachPacketS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, p -> p.particleId,
            ByteBufCodecs.INT, p -> p.entityId,
            EmitterAttachPacketS2C::new
    );

    @Override
    public Type<EmitterAttachPacketS2C> type() {
        return TYPE;
    }

    public static void handleClient(EmitterAttachPacketS2C payload, ClientPlayNetworking.Context context) {
        Player player = context.player();
        ParticleEmitter emitter = PSGameClient.LOADER.getEmitter(payload.particleId);
        Entity entity;
        if (emitter != null && (entity = player.level().getEntity(payload.entityId)) != null) {
            emitter.attachEntity(entity);
        }
    }

    public static void sendToClient(ServerPlayer serverPlayer, int particleId, Entity entity) {
        ServerPlayNetworking.send(serverPlayer, new EmitterAttachPacketS2C(particleId, entity.getId()));
    }
}

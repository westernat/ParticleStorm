package org.mesdag.particlestorm.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
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
    public @NotNull Type<EmitterAttachPacketS2C> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.isLocalPlayer()) {
                ParticleEmitter emitter = PSGameClient.LOADER.getEmitter(particleId);
                Entity entity;
                if (emitter != null && (entity = player.level().getEntity(entityId)) != null) {
                    emitter.attached = entity;
                }
            }
        }).exceptionally(e -> {
            context.disconnect(Component.translatable("neoforge.network.invalid_flow", e.getMessage()));
            return null;
        });
    }

    public static void sendToClient(ServerPlayer serverPlayer, int particleId, Entity entity) {
        PacketDistributor.sendToPlayer(serverPlayer, new EmitterAttachPacketS2C(particleId, entity.getId()));
    }
}

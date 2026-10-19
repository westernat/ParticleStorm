package org.mesdag.particlestorm.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.mesdag.particlestorm.particle.ParticleEmitter;

public record EmitterRemovalPacketS2C(int id) implements CustomPacketPayload {
    public static final Type<EmitterRemovalPacketS2C> TYPE = new Type<>(ParticleStorm.asResource("emitter_removal"));
    public static final StreamCodec<ByteBuf, EmitterRemovalPacketS2C> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(EmitterRemovalPacketS2C::new, EmitterRemovalPacketS2C::id);

    @Override
    public Type<EmitterRemovalPacketS2C> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ParticleEmitter emitter = MolangParticleEngine.INSTANCE._removeEmitter(id);
            if (emitter == null) {
                player.sendSystemMessage(Component.translatable("particle.notFound", id));
            } else {
                player.sendSystemMessage(Component.translatable("commands.particlestorm.remove", emitter.particleId == null ? id : emitter.particleId.toString()));
            }
        });
    }

    public static void sendToClient(ServerPlayer player, int id) {
        PacketDistributor.sendToPlayer(player, new EmitterRemovalPacketS2C(id));
    }
}

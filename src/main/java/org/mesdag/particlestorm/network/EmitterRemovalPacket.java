package org.mesdag.particlestorm.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.mesdag.particlestorm.GameClient;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import static org.mesdag.particlestorm.network.EmitterSynchronizePacket.KEY;

public record EmitterRemovalPacket(int id) implements CustomPacketPayload {
    public static final Type<EmitterRemovalPacket> TYPE = new Type<>(ParticleStorm.asResource("emitter_removal"));

    public static final StreamCodec<ByteBuf, EmitterRemovalPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, p -> p.id,
            EmitterRemovalPacket::new
    );

    @Override
    public @NotNull Type<EmitterRemovalPacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.isLocalPlayer()) {
                ParticleEmitter emitter = GameClient.LOADER.removeEmitter(id, false);
                if (emitter == null) {
                    player.sendSystemMessage(Component.translatable("particle.notFound", id));
                } else {
                    player.sendSystemMessage(Component.translatable("commands.particlestorm.remove", emitter.particleId == null ? id : emitter.particleId.toString()));
                }
            } else {
                CompoundTag data = player.getPersistentData();
                if (data.contains(KEY)) {
                    data.getCompound(KEY).remove(Integer.toString(id));
                }
            }
        }).exceptionally(e -> {
            context.disconnect(Component.translatable("neoforge.network.invalid_flow", e.getMessage()));
            return null;
        });
    }

    public static void sendToServer(int id) {
        PacketDistributor.sendToServer(new EmitterRemovalPacket(id));
    }

    public static void sendToClient(ServerPlayer player, int id) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(KEY)) {
            data.getCompound(KEY).remove(Integer.toString(id));
        }
        PacketDistributor.sendToPlayer(player, new EmitterRemovalPacket(id));
    }
}

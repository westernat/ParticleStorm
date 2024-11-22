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
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.particle.ParticleEmitter;

public record EmitterSynchronizePacket(int id, CompoundTag tag) implements CustomPacketPayload {
    public static final Type<EmitterSynchronizePacket> TYPE = new Type<>(ParticleStorm.asResource("emitter_synchronize"));

    public static final StreamCodec<ByteBuf, EmitterSynchronizePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, p -> p.id,
            ByteBufCodecs.COMPOUND_TAG, p -> p.tag,
            EmitterSynchronizePacket::new
    );
    public static final String KEY = "particlestorm:emitters";

    @Override
    public @NotNull Type<EmitterSynchronizePacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.isLocalPlayer()) {
                PSGameClient.LOADER.loadEmitter(player.level(), id, tag);
            } else {
                CompoundTag data = player.getPersistentData();
                if (data.contains(KEY)) {
                    data.getCompound(KEY).put(Integer.toString(id), tag);
                } else {
                    CompoundTag emitters = new CompoundTag();
                    emitters.put(Integer.toString(id), tag);
                    data.put(KEY, emitters);
                }
            }
        }).exceptionally(e -> {
            context.disconnect(Component.translatable("neoforge.network.invalid_flow", e.getMessage()));
            return null;
        });
    }

    public static void syncToServer(ParticleEmitter emitter) {
        CompoundTag tag = new CompoundTag();
        emitter.serialize(tag);
        PacketDistributor.sendToServer(new EmitterSynchronizePacket(emitter.id, tag));
    }

    public static void syncToClient(ServerPlayer player, int id) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(KEY)) {
            CompoundTag emitter = data.getCompound(KEY).getCompound(Integer.toString(id));
            PacketDistributor.sendToPlayer(player, new EmitterSynchronizePacket(id, emitter));
        } else {
            ParticleStorm.LOGGER.warn("No emitters for player: {}", player.getGameProfile());
        }
    }
}

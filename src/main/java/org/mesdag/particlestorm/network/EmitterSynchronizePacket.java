package org.mesdag.particlestorm.network;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.mixed.IPlayerPersistentData;
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
    public Type<EmitterSynchronizePacket> type() {
        return TYPE;
    }

    public static void handleClient(EmitterSynchronizePacket payload, ClientPlayNetworking.Context context) {
        Player player = context.player();
        PSGameClient.LOADER.loadEmitter(player.level(), payload.id, payload.tag);
    }

    public static void handleServer(EmitterSynchronizePacket payload, ServerPlayNetworking.Context context) {
        CompoundTag emitters = getEmitterData(context.player(), true);
        emitters.put(Integer.toString(payload.id), payload.tag.copy());
    }

    public static void syncToServer(ParticleEmitter emitter) {
        if (ClientPlayNetworking.canSend(TYPE)) {
            ClientPlayNetworking.send(new EmitterSynchronizePacket(emitter.id, emitter.serialize()));
        }
    }

    public static void syncToClient(ServerPlayer player, int id) {
        CompoundTag emitters = getEmitterData(player, false);
        if (emitters.contains(Integer.toString(id))) {
            ServerPlayNetworking.send(player, new EmitterSynchronizePacket(id, emitters.getCompoundOrEmpty(Integer.toString(id))));
        } else {
            ParticleStorm.LOGGER.warn("No persisted emitter {} for player {}", id, player.getGameProfile());
        }
    }

    public static void syncSavedEmitters(ServerPlayer player) {
        CompoundTag emitters = getEmitterData(player, false);
        for (String id : emitters.keySet()) {
            try {
                ServerPlayNetworking.send(player, new EmitterSynchronizePacket(Integer.parseInt(id), emitters.getCompoundOrEmpty(id)));
            } catch (NumberFormatException exception) {
                ParticleStorm.LOGGER.warn("Invalid persisted emitter id '{}' for player {}", id, player.getGameProfile());
            }
        }
    }

    public static CompoundTag getEmitterData(Player player, boolean create) {
        CompoundTag persistentData = IPlayerPersistentData.of(player).particlestorm$getPersistentData();
        if (!persistentData.contains(KEY) && create) {
            persistentData.put(KEY, new CompoundTag());
        }
        return persistentData.getCompoundOrEmpty(KEY);
    }
}

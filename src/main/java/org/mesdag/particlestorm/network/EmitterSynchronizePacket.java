package org.mesdag.particlestorm.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.mixed.IPlayerPersistentData;
import org.mesdag.particlestorm.particle.ParticleEmitter;

public record EmitterSynchronizePacket(int id, CompoundTag tag) implements FabricPacket {
    public static final PacketType<EmitterSynchronizePacket> TYPE = PacketType.create(ParticleStorm.asResource("emitter_synchronize"), EmitterSynchronizePacket::new);
    public static final String KEY = "particlestorm:emitters";


    public EmitterSynchronizePacket(FriendlyByteBuf buf) {
        this(buf.readInt(), java.util.Objects.requireNonNull(buf.readNbt(), "Missing emitter data"));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeNbt(tag);
    }

    @Override
    public PacketType<EmitterSynchronizePacket> getType() {
        return TYPE;
    }

    public static void handleClient(EmitterSynchronizePacket payload, Player player, PacketSender responseSender) {
        PSGameClient.LOADER.loadEmitter(player.level(), payload.id, payload.tag);
    }

    public static void handleServer(EmitterSynchronizePacket payload, ServerPlayer player, PacketSender responseSender) {
        CompoundTag emitters = getEmitterData(player, true);
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
            ServerPlayNetworking.send(player, new EmitterSynchronizePacket(id, emitters.getCompound(Integer.toString(id))));
        } else {
            ParticleStorm.LOGGER.warn("No persisted emitter {} for player {}", id, player.getGameProfile());
        }
    }

    public static void syncSavedEmitters(ServerPlayer player) {
        CompoundTag emitters = getEmitterData(player, false);
        for (String id : emitters.getAllKeys()) {
            try {
                ServerPlayNetworking.send(player, new EmitterSynchronizePacket(Integer.parseInt(id), emitters.getCompound(id)));
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
        return persistentData.getCompound(KEY);
    }
}

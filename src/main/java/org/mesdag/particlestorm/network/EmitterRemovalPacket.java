package org.mesdag.particlestorm.network;

import org.mesdag.particlestorm.particle.MolangParticleEngine;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.particle.ParticleEmitter;

public record EmitterRemovalPacket(int id) implements FabricPacket {
    public static final PacketType<EmitterRemovalPacket> TYPE = PacketType.create(ParticleStorm.asResource("emitter_removal"), EmitterRemovalPacket::new);

    public EmitterRemovalPacket(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(id);
    }

    @Override
    public PacketType<EmitterRemovalPacket> getType() {
        return TYPE;
    }

    public static void handleClient(EmitterRemovalPacket payload, Player player, PacketSender responseSender) {
        ParticleEmitter emitter = MolangParticleEngine.INSTANCE.removeEmitter(payload.id, false);
        if (emitter == null) {
            player.sendSystemMessage(Component.translatable("commands.particlestorm.not_found", payload.id));
        } else {
            player.sendSystemMessage(Component.translatable("commands.particlestorm.remove", emitter.particleId == null ? payload.id : emitter.particleId.toString()));
        }
    }

    public static void handleServer(EmitterRemovalPacket payload, ServerPlayer player, PacketSender responseSender) {
        EmitterSynchronizePacket.getEmitterData(player, false).remove(Integer.toString(payload.id));
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

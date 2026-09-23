package org.mesdag.particlestorm.network;

import org.mesdag.particlestorm.particle.MolangParticleEngine;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.particle.ParticleEmitter;

public record EmitterAttachPacketS2C(int particleId, int entityId) implements FabricPacket {
    public static final PacketType<EmitterAttachPacketS2C> TYPE = PacketType.create(ParticleStorm.asResource("emitter_attach"), EmitterAttachPacketS2C::new);

    public EmitterAttachPacketS2C(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(particleId);
        buf.writeInt(entityId);
    }

    @Override
    public PacketType<EmitterAttachPacketS2C> getType() {
        return TYPE;
    }

    public static void handleClient(EmitterAttachPacketS2C payload, Player player, PacketSender responseSender) {
        ParticleEmitter emitter = MolangParticleEngine.INSTANCE.getEmitter(payload.particleId);
        Entity entity;
        if (emitter != null && (entity = player.level().getEntity(payload.entityId)) != null) {
            emitter.attachEntity(entity);
        }
    }

    public static void sendToClient(ServerPlayer serverPlayer, int particleId, Entity entity) {
        ServerPlayNetworking.send(serverPlayer, new EmitterAttachPacketS2C(particleId, entity.getId()));
    }
}

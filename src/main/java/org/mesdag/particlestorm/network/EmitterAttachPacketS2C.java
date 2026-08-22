package org.mesdag.particlestorm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.mesdag.particlestorm.ParticleStorm;

import java.util.function.Supplier;

public record EmitterAttachPacketS2C(int particleId, int entityId) {
    public static void encode(EmitterAttachPacketS2C msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.particleId);
        buf.writeInt(msg.entityId);
    }

    public static EmitterAttachPacketS2C decode(FriendlyByteBuf buf) {
        return new EmitterAttachPacketS2C(buf.readInt(), buf.readInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PSClientPacketHandler.handleEmitterAttach(this));
    }

    public static void sendToClient(ServerPlayer player, int particleId, Entity entity) {
        ParticleStorm.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EmitterAttachPacketS2C(particleId, entity.getId()));
    }
}

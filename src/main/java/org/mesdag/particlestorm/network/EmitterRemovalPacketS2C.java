package org.mesdag.particlestorm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.mesdag.particlestorm.ParticleStorm;

import java.util.function.Supplier;

public record EmitterRemovalPacketS2C(int id) {
    public static void encode(EmitterRemovalPacketS2C msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.id);
    }

    public static EmitterRemovalPacketS2C decode(FriendlyByteBuf buf) {
        return new EmitterRemovalPacketS2C(buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PSClientPacketHandler.handleEmitterRemoval(this));
    }

    public static void sendToClient(ServerPlayer player, int id) {
        ParticleStorm.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EmitterRemovalPacketS2C(id));
    }
}

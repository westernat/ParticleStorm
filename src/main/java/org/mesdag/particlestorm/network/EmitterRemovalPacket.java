package org.mesdag.particlestorm.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.mesdag.particlestorm.ParticleStorm;

import java.util.function.Supplier;

import static org.mesdag.particlestorm.network.EmitterSynchronizePacket.KEY;

public record EmitterRemovalPacket(int id) {
    public static void encode(EmitterRemovalPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.id);
    }

    public static EmitterRemovalPacket decode(FriendlyByteBuf buf) {
        return new EmitterRemovalPacket(buf.readInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PSClientPacketHandler.handleEmitterRemoval(this));
        } else {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                CompoundTag data = player.getPersistentData();
                if (data.contains(KEY)) {
                    data.getCompound(KEY).remove(Integer.toString(id));
                }
            }
        }
    }

    public static void sendToServer(int id) {
        ParticleStorm.CHANNEL.sendToServer(new EmitterRemovalPacket(id));
    }

    public static void sendToClient(ServerPlayer player, int id) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(KEY)) {
            data.getCompound(KEY).remove(Integer.toString(id));
        }
        ParticleStorm.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EmitterRemovalPacket(id));
    }
}

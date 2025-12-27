package org.mesdag.particlestorm.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

import static org.mesdag.particlestorm.network.EmitterSynchronizePacket.KEY;

public record EmitterRemovalPacket(int id) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(id);
    }

    public static EmitterRemovalPacket decode(FriendlyByteBuf buffer) {
        return new EmitterRemovalPacket(buffer.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer sender = context.get().getSender();
            if (sender == null) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientProxy.handleRemove(id));
            } else {
                CompoundTag data = sender.getPersistentData();
                if (data.contains(KEY)) {
                    data.getCompound(KEY).remove(Integer.toString(id));
                }
            }
        });
        context.get().setPacketHandled(true);
    }

    public static void sendToServer(int id) {
        NetworkProxy.CHANNEL.send(PacketDistributor.SERVER.noArg(), new EmitterRemovalPacket(id));
    }

    public static void sendToClient(ServerPlayer player, int id) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(KEY)) {
            data.getCompound(KEY).remove(Integer.toString(id));
        }
        NetworkProxy.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EmitterRemovalPacket(id));
    }
}

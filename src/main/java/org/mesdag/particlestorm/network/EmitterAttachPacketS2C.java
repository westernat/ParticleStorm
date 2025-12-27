package org.mesdag.particlestorm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record EmitterAttachPacketS2C(int particleId, int entityId) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(particleId);
        buffer.writeVarInt(entityId);
    }

    public static EmitterAttachPacketS2C decode(FriendlyByteBuf buffer) {
        return new EmitterAttachPacketS2C(buffer.readVarInt(), buffer.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientProxy.handleAttach(particleId, entityId)));
        context.get().setPacketHandled(true);
    }

    public static void sendToClient(ServerPlayer serverPlayer, int particleId, Entity entity) {
        NetworkProxy.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new EmitterAttachPacketS2C(particleId, entity.getId()));
    }
}

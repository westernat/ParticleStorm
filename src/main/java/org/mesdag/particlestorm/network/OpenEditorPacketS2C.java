package org.mesdag.particlestorm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.mesdag.particlestorm.ParticleStorm;

import java.util.function.Supplier;

public record OpenEditorPacketS2C() {
    public static void encode(OpenEditorPacketS2C msg, FriendlyByteBuf buf) {
    }

    public static OpenEditorPacketS2C decode(FriendlyByteBuf buf) {
        return new OpenEditorPacketS2C();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> PSClientPacketHandler::handleOpenEditor));
        context.setPacketHandled(true);
    }

    public static void sendToClient(ServerPlayer player) {
        ParticleStorm.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenEditorPacketS2C());
    }
}

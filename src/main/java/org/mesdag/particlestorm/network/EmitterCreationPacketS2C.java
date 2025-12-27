package org.mesdag.particlestorm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.function.Supplier;

public record EmitterCreationPacketS2C(ResourceLocation id, Vector3f pos, MolangExp expression, int entityId) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(id);
        buffer.writeVector3f(pos);
        buffer.writeUtf(expression.getExpStr());
        buffer.writeVarInt(entityId);
    }

    public static EmitterCreationPacketS2C decode(FriendlyByteBuf buffer) {
        return new EmitterCreationPacketS2C(buffer.readResourceLocation(), buffer.readVector3f(), new MolangExp(buffer.readUtf()), buffer.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientProxy.handleCreation(id, pos, expression, entityId)));
        context.get().setPacketHandled(true);
    }

    public static void sendToAll(ResourceLocation id, Vector3f pos, MolangExp expression, @Nullable Entity entity) {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            NetworkProxy.CHANNEL.send(PacketDistributor.ALL.noArg(), new EmitterCreationPacketS2C(id, pos, expression, entity == null ? -1 : entity.getId()));
        }
    }

    public static void sendToClient(ServerPlayer player, ResourceLocation id, Vector3f pos, MolangExp expression, @Nullable Entity entity) {
        NetworkProxy.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EmitterCreationPacketS2C(id, pos, expression, entity == null ? -1 : entity.getId()));
    }
}

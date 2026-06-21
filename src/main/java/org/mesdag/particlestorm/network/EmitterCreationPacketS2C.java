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
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.function.Supplier;

public record EmitterCreationPacketS2C(ResourceLocation id, Vector3f pos, MolangExp expression, int entityId) {
    public static void encode(EmitterCreationPacketS2C msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.id);
        buf.writeFloat(msg.pos.x);
        buf.writeFloat(msg.pos.y);
        buf.writeFloat(msg.pos.z);
        msg.expression.writeToNetwork(buf);
        buf.writeVarInt(msg.entityId);
    }

    public static EmitterCreationPacketS2C decode(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        Vector3f pos = new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
        MolangExp expression = MolangExp.fromNetwork(buf);
        int entityId = buf.readVarInt();
        return new EmitterCreationPacketS2C(id, pos, expression, entityId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PSClientPacketHandler.handleEmitterCreation(this));
    }

    public static void sendToAll(ResourceLocation id, Vector3f pos, MolangExp expression, @Nullable Entity entity) {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            ParticleStorm.CHANNEL.send(PacketDistributor.ALL.noArg(),
                    new EmitterCreationPacketS2C(id, pos, expression, entity == null ? -1 : entity.getId()));
        }
    }

    public static void sendToClient(ServerPlayer player, ResourceLocation id, Vector3f pos, MolangExp expression, @Nullable Entity entity) {
        ParticleStorm.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new EmitterCreationPacketS2C(id, pos, expression, entity == null ? -1 : entity.getId()));
    }
}

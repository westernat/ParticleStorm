package org.mesdag.particlestorm.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.util.Objects;
import java.util.function.Supplier;

public record EmitterSynchronizePacket(int id, CompoundTag tag) {
    public static final String KEY = "particlestorm:emitters";

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(id);
        buffer.writeNbt(tag);
    }

    public static EmitterSynchronizePacket decode(FriendlyByteBuf buffer) {
        return new EmitterSynchronizePacket(buffer.readVarInt(), Objects.requireNonNull(buffer.readNbt()));
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer sender = context.get().getSender();
            if (sender == null) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientProxy.handleSynchronize(id, tag));
            } else {
                CompoundTag data = sender.getPersistentData();
                if (data.contains(KEY)) {
                    data.getCompound(KEY).put(Integer.toString(id), tag);
                } else {
                    CompoundTag emitters = new CompoundTag();
                    emitters.put(Integer.toString(id), tag);
                    data.put(KEY, emitters);
                }
            }
        });
        context.get().setPacketHandled(true);
    }

    public static void syncToServer(ParticleEmitter emitter) {
        CompoundTag tag = new CompoundTag();
        emitter.serialize(tag);
        NetworkProxy.CHANNEL.send(PacketDistributor.SERVER.noArg(), new EmitterSynchronizePacket(emitter.id, tag));
    }

    public static void syncToClient(ServerPlayer player, int id) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(KEY)) {
            CompoundTag emitter = data.getCompound(KEY).getCompound(Integer.toString(id));
            NetworkProxy.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EmitterSynchronizePacket(id, emitter));
        } else {
            ParticleStorm.LOGGER.warn("No emitters for player: {}", player.getGameProfile());
        }
    }
}

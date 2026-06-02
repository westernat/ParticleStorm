package org.mesdag.particlestorm.network;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.util.function.Supplier;

public record EmitterSynchronizePacket(int id, CompoundTag tag) {
    public static final String KEY = "particlestorm:emitters";

    public static void encode(EmitterSynchronizePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.id);
        buf.writeNbt(msg.tag);
    }

    public static EmitterSynchronizePacket decode(FriendlyByteBuf buf) {
        return new EmitterSynchronizePacket(buf.readInt(), buf.readNbt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Player player = Minecraft.getInstance().player;
                if (player != null) {
                    MolangParticleEngine.INSTANCE.loadEmitter(player.level(), id, tag);
                }
            });
        } else {
            Player player = ctx.get().getSender();
            if (player != null) {
                CompoundTag data = player.getPersistentData();
                if (data.contains(KEY)) {
                    data.getCompound(KEY).put(Integer.toString(id), tag);
                } else {
                    CompoundTag emitters = new CompoundTag();
                    emitters.put(Integer.toString(id), tag);
                    data.put(KEY, emitters);
                }
            }
        }
    }

    public static void syncToServer(ParticleEmitter emitter) {
        CompoundTag tag = new CompoundTag();
        emitter.serialize(tag);
        ParticleStorm.CHANNEL.sendToServer(new EmitterSynchronizePacket(emitter.id, tag));
    }

    public static void syncToClient(ServerPlayer player, int id) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(KEY)) {
            CompoundTag emitter = data.getCompound(KEY).getCompound(Integer.toString(id));
            ParticleStorm.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EmitterSynchronizePacket(id, emitter));
        } else {
            ParticleStorm.LOGGER.warn("No emitters for player: {}", player.getGameProfile());
        }
    }
}

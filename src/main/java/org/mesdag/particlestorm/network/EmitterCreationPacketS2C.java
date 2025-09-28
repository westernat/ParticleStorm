package org.mesdag.particlestorm.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.ParticleEmitter;

public record EmitterCreationPacketS2C(ResourceLocation id, Vector3f pos, MolangExp expression) implements CustomPacketPayload {
    public static final Type<EmitterCreationPacketS2C> TYPE = new Type<>(ParticleStorm.asResource("emitter_creation"));

    public static final StreamCodec<ByteBuf, EmitterCreationPacketS2C> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, EmitterCreationPacketS2C::id,
            ByteBufCodecs.VECTOR3F, EmitterCreationPacketS2C::pos,
            MolangExp.STREAM_CODEC, EmitterCreationPacketS2C::expression,
            EmitterCreationPacketS2C::new
    );

    @Override
    public @NotNull Type<EmitterCreationPacketS2C> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.isLocalPlayer()) {
                ParticleEmitter emitter = new ParticleEmitter(player.level(), new Vec3(pos.x, pos.y, pos.z), id, expression);
                PSGameClient.LOADER.addEmitter(emitter, false);
            }
        }).exceptionally(e -> {
            context.disconnect(Component.translatable("neoforge.network.invalid_flow", e.getMessage()));
            return null;
        });
    }

    public static void sendToAll(ResourceLocation id, Vector3f pos, MolangExp expression) {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            PacketDistributor.sendToAllPlayers(new EmitterCreationPacketS2C(id, pos, expression));
        }
    }

    public static void sendToClient(ServerPlayer player, ResourceLocation id, Vector3f pos, MolangExp expression) {
        PacketDistributor.sendToPlayer(player, new EmitterCreationPacketS2C(id, pos, expression));
    }
}

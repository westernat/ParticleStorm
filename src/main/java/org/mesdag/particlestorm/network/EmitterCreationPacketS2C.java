package org.mesdag.particlestorm.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.mesdag.particlestorm.GameClient;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.data.event.ParticleEffect;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.ParticleEmitter;

public record EmitterCreationPacketS2C(ResourceLocation id, Vector3f pos, ParticleEffect.Type effectType, MolangExp expression) implements CustomPacketPayload {
    public static final Type<EmitterCreationPacketS2C> TYPE = new Type<>(ParticleStorm.asResource("emitter_creation"));

    public static final StreamCodec<ByteBuf, EmitterCreationPacketS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, p -> p.id.toString(),
            ByteBufCodecs.VECTOR3F, p -> p.pos,
            ParticleEffect.Type.STREAM_CODEC, p -> p.effectType,
            MolangExp.STREAM_CODEC, p -> p.expression,
            (s, v, t, e) -> new EmitterCreationPacketS2C(ResourceLocation.parse(s), v, t, e)
    );

    @Override
    public @NotNull Type<EmitterCreationPacketS2C> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.isLocalPlayer()) {
                ParticleEmitter emitter = new ParticleEmitter(player.level(), new Vec3(pos.x, pos.y, pos.z), id, effectType, expression);
                GameClient.LOADER.addEmitter(emitter, true);
            }
        }).exceptionally(e -> {
            context.disconnect(Component.translatable("neoforge.network.invalid_flow", e.getMessage()));
            return null;
        });
    }
}

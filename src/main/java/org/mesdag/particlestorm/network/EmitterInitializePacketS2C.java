package org.mesdag.particlestorm.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.data.event.ParticleEffect;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.ParticleEmitterEntity;

public record EmitterInitializePacketS2C(int id, ResourceLocation particle, ParticleEffect.Type effectType, MolangExp expression) implements CustomPacketPayload {
    public static final ResourceLocation REQUEST_ID = ParticleStorm.asResource("./request");
    public static final Type<EmitterInitializePacketS2C> TYPE = new Type<>(ParticleStorm.asResource("emitter_initialize"));

    public static final StreamCodec<ByteBuf, EmitterInitializePacketS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, p -> p.id,
            ByteBufCodecs.STRING_UTF8, p -> p.particle.toString(),
            ParticleEffect.Type.STREAM_CODEC, p -> p.effectType,
            MolangExp.STREAM_CODEC, p -> p.expression,
            (i, s, t, e) -> new EmitterInitializePacketS2C(i, ResourceLocation.parse(s), t, e)
    );

    @Override
    public @NotNull Type<EmitterInitializePacketS2C> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();
            if(level.getEntity(id) instanceof ParticleEmitterEntity entity) {
                if (context.player() instanceof ServerPlayer serverPlayer) {
                    if (particle.equals(REQUEST_ID)) {
                        PacketDistributor.sendToPlayer(serverPlayer, new EmitterInitializePacketS2C(id, entity.particleId, effectType, expression));
                    }
                } else {
                    entity.particleId = particle;
                    entity.effectType = effectType;
                    entity.expression = expression;
                }
            }
        }).exceptionally(e -> {
            context.disconnect(Component.translatable("neoforge.network.invalid_flow", e.getMessage()));
            return null;
        });
    }
}

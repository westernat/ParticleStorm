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
import org.mesdag.particlestorm.particle.ParticleEmitterEntity;

public record EmitterParticlePacketS2C(int id, ResourceLocation particle) implements CustomPacketPayload {
    public static final ResourceLocation REQUEST_ID = ParticleStorm.asResource("./request");
    public static final CustomPacketPayload.Type<EmitterParticlePacketS2C> TYPE = new Type<>(ParticleStorm.asResource("emitter_particle"));

    public static final StreamCodec<ByteBuf, EmitterParticlePacketS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, p -> p.id,
            ByteBufCodecs.STRING_UTF8, p -> p.particle.toString(),
            (i, s) -> new EmitterParticlePacketS2C(i, ResourceLocation.parse(s))
    );

    @Override
    public @NotNull Type<EmitterParticlePacketS2C> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();
            if(level.getEntity(id) instanceof ParticleEmitterEntity entity) {
                if (context.player() instanceof ServerPlayer serverPlayer) {
                    if (particle.equals(REQUEST_ID)) {
                        PacketDistributor.sendToPlayer(serverPlayer, new EmitterParticlePacketS2C(id, entity.particleId));
                    }
                } else {
                    entity.particleId = particle;
                }
            }
        }).exceptionally(e -> {
            context.disconnect(Component.translatable("neoforge.network.invalid_flow", e.getMessage()));
            return null;
        });
    }
}

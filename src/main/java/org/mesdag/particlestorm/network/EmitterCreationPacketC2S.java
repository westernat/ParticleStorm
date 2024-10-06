package org.mesdag.particlestorm.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.data.event.ParticleEffect;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.MolangParticleOption;
import org.mesdag.particlestorm.particle.ParticleEmitterEntity;

import java.util.Collections;

public record EmitterCreationPacketC2S(ResourceLocation particle, Vec3 pos, ParticleEffect.Type effectType, MolangExp expression) implements CustomPacketPayload {
    public static final Type<EmitterCreationPacketC2S> TYPE = new Type<>(ParticleStorm.asResource("emitter_creation"));

    public static final StreamCodec<ByteBuf, EmitterCreationPacketC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, p -> p.particle.toString(),
            ByteBufCodecs.VECTOR3F, p -> new Vector3f((float) p.pos.x, (float) p.pos.y, (float) p.pos.z),
            ParticleEffect.Type.STREAM_CODEC, p -> p.effectType,
            MolangExp.STREAM_CODEC, p -> p.expression,
            (s, v, t, e) -> new EmitterCreationPacketC2S(ResourceLocation.parse(s), new Vec3(v.x, v.y, v.z), t, e)
    );

    @Override
    public @NotNull Type<EmitterCreationPacketC2S> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                ServerLevel serverLevel = serverPlayer.serverLevel();
                ParticleEmitterEntity.ManualData manualData = new ParticleEmitterEntity.ManualData(serverLevel, new MolangParticleOption(particle), pos, Vec3.ZERO, 0, 1, false, Collections.singleton(serverPlayer));
                ParticleEmitterEntity entity = new ParticleEmitterEntity(serverLevel, manualData, particle, pos, effectType, expression);
                serverLevel.addFreshEntity(entity);
            }
        }).exceptionally(e -> {
            context.disconnect(Component.translatable("neoforge.network.invalid_flow", e.getMessage()));
            return null;
        });
    }
}

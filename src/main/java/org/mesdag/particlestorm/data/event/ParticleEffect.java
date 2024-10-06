package org.mesdag.particlestorm.data.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.MolangInstance;
import org.mesdag.particlestorm.network.EmitterCreationPacketC2S;

import java.util.Locale;
import java.util.function.IntFunction;

public record ParticleEffect(ResourceLocation effect, Type type, MolangExp preEffectExpression) implements IEventNode {
    public static final MapCodec<ParticleEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("effect").forGetter(ParticleEffect::effect),
            Type.CODEC.fieldOf("type").forGetter(ParticleEffect::type),
            MolangExp.CODEC.fieldOf("pre_effect_expression").orElse(MolangExp.EMPTY).forGetter(ParticleEffect::preEffectExpression)
    ).apply(instance, ParticleEffect::new));

    @Override
    public void execute(MolangInstance instance) {
        PacketDistributor.sendToServer(new EmitterCreationPacketC2S(effect, instance.getPosition(), type, preEffectExpression));
    }

    public enum Type implements StringRepresentable {
        EMITTER,
        EMITTER_BOUND,
        PARTICLE,
        PARTICLE_WITH_VELOCITY;

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);
        public static final StreamCodec<ByteBuf, Type> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Enum::ordinal,
                Type::getById
        );
        private static final IntFunction<Type> BY_ID = ByIdMap.continuous(Type::getId, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);

        public int getId() {
            return ordinal();
        }

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Type getById(int id) {
            return BY_ID.apply(id);
        }
    }
}

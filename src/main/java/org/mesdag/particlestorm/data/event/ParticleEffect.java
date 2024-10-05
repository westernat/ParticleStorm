package org.mesdag.particlestorm.data.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.Locale;

public record ParticleEffect(ResourceLocation effect, Type type, MolangExp preEffectExpression) implements IEventNode {
    public static final MapCodec<ParticleEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("effect").forGetter(ParticleEffect::effect),
            Type.CODEC.fieldOf("type").forGetter(ParticleEffect::type),
            MolangExp.CODEC.fieldOf("pre_effect_expression").orElse(MolangExp.EMPTY).forGetter(ParticleEffect::preEffectExpression)
    ).apply(instance, ParticleEffect::new));

    public enum Type implements StringRepresentable {
        EMITTER,
        EMITTER_BOUND,
        PARTICLE,
        PARTICLE_WITH_VELOCITY;

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}

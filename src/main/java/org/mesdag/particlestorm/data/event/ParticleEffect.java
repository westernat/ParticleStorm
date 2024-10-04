package org.mesdag.particlestorm.data.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.Locale;

public record ParticleEffect(ResourceLocation effect, Type type, MolangExp preEffectExpression) implements IEventNode {
    public static final ParticleEffect EMPTY = new ParticleEffect(ParticleStorm.EMPTY_LOCATION, Type.EMITTER, MolangExp.EMPTY);
    public static final MapCodec<ParticleEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("effect").forGetter(ParticleEffect::effect),
            Type.CODEC.fieldOf("type").forGetter(ParticleEffect::type),
            MolangExp.CODEC.fieldOf("pre_effect_expression").forGetter(ParticleEffect::preEffectExpression)
    ).apply(instance, ParticleEffect::new));

    @Override
    public MapCodec<ParticleEffect> codec() {
        return CODEC;
    }

    @Override
    public String name() {
        return "particle_effect";
    }

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

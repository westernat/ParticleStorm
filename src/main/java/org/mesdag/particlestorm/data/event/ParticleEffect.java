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
import org.jetbrains.annotations.NotNull;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;

public record ParticleEffect(ResourceLocation effect, Type type, MolangExp preEffectExpression, List<String> sharedVars) implements IEventNode {
    public static final MapCodec<ParticleEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("effect").forGetter(ParticleEffect::effect),
            Type.CODEC.fieldOf("type").forGetter(ParticleEffect::type),
            MolangExp.CODEC.fieldOf("pre_effect_expression").orElse(MolangExp.EMPTY).forGetter(ParticleEffect::preEffectExpression),
            Codec.STRING.listOf().lenientOptionalFieldOf("shared_vars", List.of()).forGetter(ParticleEffect::sharedVars)
    ).apply(instance, ParticleEffect::new));

    public ParticleEffect(ResourceLocation effect, Type type, MolangExp preEffectExpression) {
        this(effect, type, preEffectExpression, List.of());
    }

    @Override
    public void execute(MolangInstance instance) {
        ParticleEmitter emitter = new ParticleEmitter(instance.getLevel(), instance.getPosition(), effect, preEffectExpression);
        emitter.afterParentInit = parent -> {
            switch (type) {
                case EMITTER -> {}
                case EMITTER_BOUND -> {
                    emitter.attachEntity(parent.getAttachedEntity());
                    emitter.attachedBlock = parent.attachedBlock;
                    emitter.offsetPos = parent.offsetPos;
                    emitter.offsetRot = parent.offsetRot;
                    emitter.parentPosition = parent.parentPosition;
                    emitter.parentRotation = parent.parentRotation;
                    emitter.parentMode = parent.parentMode;
                }
                case PARTICLE -> emitter.isManual = true;
                case PARTICLE_WITH_VELOCITY -> {
                    emitter.isManual = true;
                    if (parent.getAttachedEntity() != null) {
                        emitter.inheritedParticleSpeed = parent.getAttachedEntity().getDeltaMovement().toVector3f();
                    }
                }
            }
        };
        ParticleEmitter parent = instance.getEmitter();
        emitter.addParent(parent);
        for (String name : sharedVars) {
            Variable variable = parent.getVars().table.get(name);
            if (variable == null) throw new IllegalArgumentException("Shared vars must defined in parent directly!");
            emitter.getVars().setValue(name, variable);
        }
        PSGameClient.LOADER.addEmitter(emitter, false);
    }

    @Override
    public String toString() {
        return "ParticleEffect{" +
                "effect=" + effect +
                ", type=" + type +
                ", preEffectExpression=" + preEffectExpression +
                ", sharedVars=" + sharedVars +
                '}';
    }

    public enum Type implements StringRepresentable {
        /**
         * Create an emitter of the specified particle effect at the event's world location
         */
        EMITTER,
        /**
         * Create an emitter of the specified particle effect at the event's location.
         * <p>
         * If the firing emitter is bound to an entity or locator, the new emitter will be bound to the same entity or locator.
         */
        EMITTER_BOUND,
        /**
         * Manually emit a particle on an emitter of the specified type at the event location, creating the emitter if it doesn't already exist.
         * <p>
         * Make sure to use the Spawn Amount mode "Manual" on the child particle effect.
         */
        PARTICLE,
        /**
         * The same as "Particle" except the new particle will inherit the spawning particle's velocity.
         */
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

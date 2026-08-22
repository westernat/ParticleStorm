package org.mesdag.particlestorm.data.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.api.RegisterCustomEmitterTypeEvent;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.compiler.MolangQueries;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;

public record ParticleEffect(ResourceLocation effect, Type type, MolangExp preEffectExpression, List<String> sharedVars) implements IEventNode {
    public static final Codec<ParticleEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("effect").forGetter(ParticleEffect::effect),
            Type.CODEC.fieldOf("type").forGetter(ParticleEffect::type),
            MolangExp.CODEC.fieldOf("pre_effect_expression").orElse(MolangExp.EMPTY).forGetter(ParticleEffect::preEffectExpression),
            Codec.STRING.listOf().optionalFieldOf("shared_vars", List.of()).forGetter(ParticleEffect::sharedVars)
    ).apply(instance, (rl, t, e, l) -> {
        l = l.stream().map(s -> MolangQueries.applyPrefixAliases(s, "variable.", "v.")).toList();
        return new ParticleEffect(rl, t, e, l);
    }));

    public ParticleEffect(ResourceLocation effect, Type type, MolangExp preEffectExpression) {
        this(effect, type, preEffectExpression, List.of());
    }

    private static final Vector3f vector3f = new Vector3f();

    @Override
    public void execute(MolangInstance instance) {
        ParticleEmitter emitter = RegisterCustomEmitterTypeEvent.create(instance.getEmitter(), this);
        if (instance instanceof IMolangParticleInstance p) {
            p.getEmitter().local2World(vector3f.set((float) p.getX(), (float) p.getY(), (float) p.getZ()), 1);
            emitter.setPos(new Vec3(vector3f.x, vector3f.y, vector3f.z));
            emitter.posO = emitter.getPosition();
        }
        MolangParticleEngine.INSTANCE.addEmitter(emitter);
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
        /// Create an emitter of the specified particle effect at the event's world location
        EMITTER,
        /// Create an emitter of the specified particle effect at the event's location.
        ///
        /// If the firing emitter is bound to an entity or locator, the new emitter will be bound to the same entity or locator.
        EMITTER_BOUND,
        /// Manually emit a particle on an emitter of the specified type at the event location, creating the emitter if it doesn't already exist.
        ///
        /// Make sure to use the Spawn Amount mode "Manual" on the child particle effect.
        PARTICLE,
        /// The same as "Particle" except the new particle will inherit the spawning particle's velocity.
        PARTICLE_WITH_VELOCITY;

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);
        private static final IntFunction<Type> BY_ID = ByIdMap.continuous(Type::getId, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);

        public int getId() {
            return ordinal();
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Type getById(int id) {
            return BY_ID.apply(id);
        }
    }
}

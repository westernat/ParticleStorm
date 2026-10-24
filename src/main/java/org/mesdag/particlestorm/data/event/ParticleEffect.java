package org.mesdag.particlestorm.data.event;


import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.api.RegisterCustomEmitterTypeEvent;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.compiler.MolangQueries;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.util.*;
import java.util.function.IntFunction;

public record ParticleEffect(ResourceLocation effect, Type type, MolangExp preEffectExpression,
                             List<SharedVar> sharedVars) implements IEventNode {
    /// 一条共享变量声明。
    ///
    /// 兼容两种 JSON 写法：
    /// - 旧格式（Bedrock）：`"shared_vars": ["v.a", "v.b"]`，`defaultValue == null`；
    /// - 扩展格式：`"shared_vars": {"v.a": 1, "v.b": "v.a + 1"}`，值是可选的默认 Molang 表达式。
    ///
    /// 父发射器里没有定义该变量时，用默认值在父/子变量表里补一个共享变量；没有默认值则补 0（不再抛异常）。
    public record SharedVar(String name, @Nullable FloatMolangExp defaultValue) {
        public SharedVar {
            name = normalize(name);
        }

        public SharedVar(String name) {
            this(name, null);
        }
    }

    private static final Codec<List<SharedVar>> SHARED_VARS_CODEC = Codec.either(
            Codec.unboundedMap(Codec.STRING, FloatMolangExp.CODEC),
            Codec.STRING.listOf()
    ).xmap(
            either -> either.map(ParticleEffect::fromMap, ParticleEffect::fromNames),
            ParticleEffect::toEither
    );

    public static final Codec<ParticleEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("effect").forGetter(ParticleEffect::effect),
            Type.CODEC.fieldOf("type").forGetter(ParticleEffect::type),
            ParticleStorm.lenientOptionalFieldOf(MolangExp.CODEC, "pre_effect_expression", () -> MolangExp.EMPTY).forGetter(ParticleEffect::preEffectExpression),
            SHARED_VARS_CODEC.optionalFieldOf("shared_vars", List.of()).forGetter(ParticleEffect::sharedVars)
    ).apply(instance, ParticleEffect::new));

    private static List<SharedVar> fromMap(Map<String, FloatMolangExp> map) {
        List<SharedVar> vars = new ArrayList<>(map.size());
        map.forEach((name, defaultValue) -> {
            String normalized = normalize(name);
            if (!normalized.isEmpty()) {
                vars.add(new SharedVar(normalized, defaultValue));
            }
        });
        return vars;
    }

    private static List<SharedVar> fromNames(List<String> names) {
        List<SharedVar> vars = new ArrayList<>(names.size());
        for (String name : names) {
            String normalized = normalize(name);
            if (!normalized.isEmpty()) {
                vars.add(new SharedVar(normalized));
            }
        }
        return vars;
    }

    private static Either<Map<String, FloatMolangExp>, List<String>> toEither(List<SharedVar> vars) {
        boolean hasDefault = vars.stream().anyMatch(var -> var.defaultValue() != null);
        if (!hasDefault) {
            return Either.right(vars.stream().map(SharedVar::name).toList());
        }
        Map<String, FloatMolangExp> map = new LinkedHashMap<>();
        for (SharedVar var : vars) {
            map.put(var.name(), var.defaultValue() == null ? FloatMolangExp.ZERO : var.defaultValue());
        }
        return Either.left(map);
    }

    private static String normalize(String name) {
        return MolangQueries.applyPrefixAliases(name.trim(), "variable.", "v.");
    }

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
        MolangParticleEngine.addEmitterRouted(emitter);
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

package org.mesdag.particlestorm.data.event;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.GsonHelper;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.compiler.MolangQueries;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

public record ParticleEffect(ResourceLocation effect, Type type, MolangExp preEffectExpression, List<String> sharedVars) implements IEventNode {
    public ParticleEffect(ResourceLocation effect, Type type, MolangExp preEffectExpression) {
        this(effect, type, preEffectExpression, List.of());
    }

    @Override
    public void execute(MolangInstance instance) {
        PSGameClient.LOADER.addEmitter(new ParticleEmitter(instance.getEmitter(), this), false);
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

    public static ParticleEffect fromJson(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        ResourceLocation effect = new ResourceLocation(GsonHelper.getAsString(object, "effect"));
        Type type = Type.fromJson(GsonHelper.getNonNull(object, "type"));
        MolangExp exp = MolangExp.fromJson(object.get("pre_effect_expression"));
        JsonElement jsonElement = object.get("shared_vars");
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        if (jsonElement != null) {
            for (JsonElement jsonElement1 : jsonElement.getAsJsonArray()) {
                builder.add(MolangQueries.applyPrefixAliases(jsonElement1.getAsString(), "variable.", "v."));
            }
        }
        return new ParticleEffect(effect, type, exp, builder.build());
    }

    public enum Type {
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

        private static final Map<String, Type> MAP = Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(
                e -> e.name().toLowerCase(Locale.ROOT),
                Function.identity()
        ));
        private static final IntFunction<Type> BY_ID = ByIdMap.continuous(Type::getId, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);

        public int getId() {
            return ordinal();
        }

        public static Type getById(int id) {
            return BY_ID.apply(id);
        }

        public static Type fromJson(JsonElement element) {
            return MAP.getOrDefault(element.getAsString(), EMITTER);
        }
    }
}

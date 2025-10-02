package org.mesdag.particlestorm.data.molang.compiler;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModLoader;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.api.RegisterMolangQueriesEvent;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.mixin.ParticleEngineAccessor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToDoubleFunction;

public final class MolangQueries {
    private static final Map<String, Variable> UNFROZEN_QUERIES = new ConcurrentHashMap<>();
    private static final Map<String, Variable> FROZEN_QUERIES = new HashMap<>();

    static {
        setDefaultQueryValues();
    }

    public static boolean isExistingVariable(String name) {
        return FROZEN_QUERIES.containsKey(name);
    }

    @Deprecated
    public static void registerVariable(String name, Variable variable) {
        checkFrozen();
        UNFROZEN_QUERIES.put(name, variable);
    }

    static Variable getQueryFor(String name) {
        return FROZEN_QUERIES.getOrDefault(applyPrefixAliases(name, "query.", "q."), new Variable(name, 0));
    }

    private static void registerQueryVariable(String name, ToDoubleFunction<MolangInstance> value) {
        checkFrozen();
        checkUnregistered(name);
        UNFROZEN_QUERIES.put(name, new Variable(name, value));
    }

    private static void checkUnregistered(String name) {
        if (UNFROZEN_QUERIES.containsKey(name)) {
            throw new IllegalArgumentException(name + " had already registered!");
        }
    }

    private static void checkFrozen() {
        if (!FROZEN_QUERIES.isEmpty()) throw new UnsupportedOperationException("Had already frozen!");
    }

    /**
     * Parse a given string formatted with a prefix, swapping out any potential aliases for the defined proper name
     *
     * @param text       The base text to parse
     * @param properName The "correct" prefix to apply
     * @param aliases    The available prefixes to check and replace
     * @return The unaliased string, or the original string if no aliases match
     */
    public static String applyPrefixAliases(String text, String properName, String... aliases) {
        for (String alias : aliases) {
            if (text.startsWith(alias))
                return properName + text.substring(alias.length());
        }

        return text;
    }

    private static void setDefaultQueryValues() {
        registerQueryVariable("query.cardinal_player_facing", p -> Minecraft.getInstance().player == null ? 0.0 : Minecraft.getInstance().player.getDirection().ordinal());
        registerQueryVariable("query.day", p -> p.getLevel().getGameTime() / 24000d);
        registerQueryVariable("query.has_cape", p -> Minecraft.getInstance().player == null ? 0.0 : Minecraft.getInstance().player.getSkin().capeTexture() == null ? 0 : 1);
        registerQueryVariable("query.is_first_person", p -> Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON ? 1 : 0);
        registerQueryVariable("query.moon_brightness", p -> p.getLevel().getMoonBrightness());
        registerQueryVariable("query.moon_phase", p -> p.getLevel().getMoonPhase());
        registerQueryVariable("query.player_level", p -> Minecraft.getInstance().player == null ? 0.0 : Minecraft.getInstance().player.experienceLevel);
        registerQueryVariable("query.time_of_day", p -> p.getLevel().getDayTime() / 24000f);
        registerQueryVariable("query.time_stamp", p -> p.getLevel().getGameTime());
        registerQueryVariable("query.total_emitter_count", p -> PSGameClient.LOADER.totalEmitterCount());
        registerQueryVariable("query.total_particle_count", p -> {
            int sum = 0;
            for (Integer value : ((ParticleEngineAccessor) Minecraft.getInstance().particleEngine).trackedParticleCounts().values()) {
                sum += value;
            }
            return sum;
        });
        registerQueryVariable("query.attached_x", p -> p.getAttachedEntity() == null ? 0.0 : p.getAttachedEntity().getX());
        registerQueryVariable("query.attached_y", p -> p.getAttachedEntity() == null ? 0.0 : p.getAttachedEntity().getY());
        registerQueryVariable("query.attached_z", p -> p.getAttachedEntity() == null ? 0.0 : p.getAttachedEntity().getZ());
        registerQueryVariable("query.attached_xo", p -> p.getAttachedEntity() == null ? 0.0 : p.getAttachedEntity().xo);
        registerQueryVariable("query.attached_yo", p -> p.getAttachedEntity() == null ? 0.0 : p.getAttachedEntity().yo);
        registerQueryVariable("query.attached_zo", p -> p.getAttachedEntity() == null ? 0.0 : p.getAttachedEntity().zo);
        ModLoader.postEvent(new RegisterMolangQueriesEvent(MolangQueries::registerQueryVariable));
        FROZEN_QUERIES.putAll(UNFROZEN_QUERIES);
        UNFROZEN_QUERIES.clear();
    }
}

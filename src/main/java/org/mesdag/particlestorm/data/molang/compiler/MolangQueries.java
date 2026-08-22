package org.mesdag.particlestorm.data.molang.compiler;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModLoader;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.api.RegisterMolangQueriesEvent;
import org.mesdag.particlestorm.api.ToFloatFunction;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.particle.MolangParticleEngine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MolangQueries {
    private static Map<String, Variable> UNFROZEN_QUERIES = new ConcurrentHashMap<>();
    private static Map<String, Variable> FROZEN_QUERIES = ImmutableMap.of();

    static {
        setDefaultQueryValues();
    }

    public static boolean isExistingVariable(String name) {
        return FROZEN_QUERIES.containsKey(name);
    }

    static Variable getQueryFor(String name) {
        return FROZEN_QUERIES.getOrDefault(applyQueryAliases(name), new Variable(name, 0));
    }

    private static void registerQueryVariable(String name, ToFloatFunction<MolangInstance> value) {
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

    /// Parse a given string formatted with a prefix, swapping out any potential aliases for the defined proper name
    ///
    /// @param text       The base text to parse
    /// @param properName The "correct" prefix to apply
    /// @param aliases    The available prefixes to check and replace
    /// @return The unaliased string, or the original string if no aliases match
    public static String applyPrefixAliases(String text, String properName, String... aliases) {
        for (String alias : aliases) {
            if (text.startsWith(alias))
                return properName + text.substring(alias.length());
        }

        return text;
    }

    public static String applyQueryAliases(String text) {
        if (text.startsWith("q.")) {
            return "query" + text.substring(1);
        }
        return text;
    }

    private static void setDefaultQueryValues() {
        registerQueryVariable("query.cardinal_player_facing", p -> Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.getDirection().ordinal());
        registerQueryVariable("query.day", p -> p.getLevel().getGameTime() / 24000F);
        registerQueryVariable("query.has_cape", p -> 0);
        registerQueryVariable("query.is_first_person", p -> Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON ? 1 : 0);
        registerQueryVariable("query.moon_brightness", p -> p.getLevel().getMoonBrightness());
        registerQueryVariable("query.moon_phase", p -> p.getLevel().getMoonPhase());
        registerQueryVariable("query.player_level", p -> Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.experienceLevel);
        registerQueryVariable("query.time_of_day", p -> p.getLevel().getDayTime() / 24000f);
        registerQueryVariable("query.time_stamp", p -> p.getLevel().getGameTime());
        registerQueryVariable("query.total_emitter_count", p -> MolangParticleEngine.INSTANCE.totalEmitterCount());
        registerQueryVariable("query.total_particle_count", p -> MolangParticleEngine.INSTANCE.totalParticleCount());
        registerQueryVariable("query.attached_x", p -> p.getAttachedEntity() == null ? 0 : (float) p.getAttachedEntity().getX());
        registerQueryVariable("query.attached_y", p -> p.getAttachedEntity() == null ? 0 : (float) p.getAttachedEntity().getY());
        registerQueryVariable("query.attached_z", p -> p.getAttachedEntity() == null ? 0 : (float) p.getAttachedEntity().getZ());
        registerQueryVariable("query.attached_xo", p -> p.getAttachedEntity() == null ? 0 : (float) p.getAttachedEntity().xo);
        registerQueryVariable("query.attached_yo", p -> p.getAttachedEntity() == null ? 0 : (float) p.getAttachedEntity().yo);
        registerQueryVariable("query.attached_zo", p -> p.getAttachedEntity() == null ? 0 : (float) p.getAttachedEntity().zo);
        ModLoader.get().postEvent(new RegisterMolangQueriesEvent(MolangQueries::registerQueryVariable));
        FROZEN_QUERIES = ImmutableMap.copyOf(UNFROZEN_QUERIES);
        UNFROZEN_QUERIES = null;
    }
}

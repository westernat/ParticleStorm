package org.mesdag.particlestorm.data.molang.compiler;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.mixin.ParticleEngineAccessor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MolangQueries {
    private static final Map<String, Variable> QUERIES = new ConcurrentHashMap<>();

    static {
        setDefaultQueryValues();
    }

    public static boolean isExistingVariable(String name) {
        return QUERIES.containsKey(name);
    }

    public static void registerVariable(String name, Variable variable) {
        QUERIES.put(name, variable);
    }

    static Variable getQueryFor(String name) {
        return QUERIES.computeIfAbsent(applyPrefixAliases(name, "query.", "q."), key -> new Variable(key, 0));
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
        getQueryFor("query.cardinal_player_facing").set(p -> Minecraft.getInstance().player == null ? 0.0 : Minecraft.getInstance().player.getDirection().ordinal());
        getQueryFor("query.day").set(p -> p.getLevel().getGameTime() / 24000d);
        getQueryFor("query.has_cape").set(p -> Minecraft.getInstance().player == null ? 0.0 : Minecraft.getInstance().player.getSkin().capeTexture() == null ? 0 : 1);
        getQueryFor("query.is_first_person").set(p -> Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON ? 1 : 0);
        getQueryFor("query.moon_brightness").set(p -> p.getLevel().getMoonBrightness());
        getQueryFor("query.moon_phase").set(p -> p.getLevel().getMoonPhase());
        getQueryFor("query.player_level").set(p -> Minecraft.getInstance().player == null ? 0.0 : Minecraft.getInstance().player.experienceLevel);
        getQueryFor("query.time_of_day").set(p -> p.getLevel().getDayTime() / 24000f);
        getQueryFor("query.time_stamp").set(p -> p.getLevel().getGameTime());
        getQueryFor("query.total_emitter_count").set(p -> PSGameClient.LOADER.totalEmitterCount());
        getQueryFor("query.total_particle_count").set(p -> {
            int sum = 0;
            for (Integer value : ((ParticleEngineAccessor) Minecraft.getInstance().particleEngine).trackedParticleCounts().values()) {
                sum += value;
            }
            return sum;
        });
        getQueryFor("query.attached_x").set(p -> p.getAttachedEntity() == null ? 0.0 : p.getAttachedEntity().getX());
        getQueryFor("query.attached_y").set(p -> p.getAttachedEntity() == null ? 0.0 : p.getAttachedEntity().getY());
        getQueryFor("query.attached_z").set(p -> p.getAttachedEntity() == null ? 0.0 : p.getAttachedEntity().getZ());
        getQueryFor("query.attached_xo").set(p -> p.getAttachedEntity() == null ? 0.0 : p.getAttachedEntity().xo);
        getQueryFor("query.attached_yo").set(p -> p.getAttachedEntity() == null ? 0.0 : p.getAttachedEntity().yo);
        getQueryFor("query.attached_zo").set(p -> p.getAttachedEntity() == null ? 0.0 : p.getAttachedEntity().zo);
    }
}

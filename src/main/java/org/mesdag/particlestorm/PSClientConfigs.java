package org.mesdag.particlestorm;

import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class PSClientConfigs {
    private static final String DEBUG = "debug";
    private static final String SHOW_EMITTER_OUTLINE = "showEmitterOutline";
    private static final String MAX_TRACKERS_PER_ENTITY = "maxTrackersPerEntity";
    private static final String EMITTER_LIMIT = "emitterLimit";
    private static final String FPS_THRESHOLD = "fpsThreshold";
    private static final String ALLOWS_VANILLA_PARTICLE_WHEN_REACH_LIMIT = "allowsVanillaParticleWhenReachLimit";
    private static final String EMITTER_AUTO_REMOVE_INTERVAL_TICK = "emitterAutoRemoveIntervalTick";
    private static final String EMITTER_AUTO_REMOVE_MINIMUM_DISTANCE = "minimumEmitterAutoRemoveDistance";
    private static final String EMITTER_AUTO_REMOVE_ATTENUATION_DISTANCE = "minimumEmitterAutoRemoveAttenuationDistance";
    private static final String EMITTER_AUTO_REMOVE_ATTENUATION_COEFFICIENT = "minimumEmitterAutoRemoveAttenuationCoefficient";

    public static boolean debug = false;
    public static boolean showEmitterOutline = true;
    public static int maxTrackersPerEntity = 64;
    public static int emitterLimit = 50;
    public static int fpsThreshold = 30;
    public static boolean allowsVanillaParticleWhenReachLimit = false;
    public static int emitterAutoRemoveIntervalTick = 1;
    public static int emitterAutoRemoveMinimumDistance = 32;
    public static int emitterAutoRemoveAttenuationDistance = 16;
    public static double emitterAutoRemoveAttenuationCoefficient = 0.25;

    private PSClientConfigs() {
    }

    public static void onLoad() {
        Properties properties = new Properties();
        properties.setProperty(DEBUG, "false");
        properties.setProperty(SHOW_EMITTER_OUTLINE, "true");
        properties.setProperty(MAX_TRACKERS_PER_ENTITY, "64");
        properties.setProperty(EMITTER_LIMIT, "50");
        properties.setProperty(FPS_THRESHOLD, "30");
        properties.setProperty(ALLOWS_VANILLA_PARTICLE_WHEN_REACH_LIMIT, "false");
        properties.setProperty(EMITTER_AUTO_REMOVE_INTERVAL_TICK, "1");
        properties.setProperty(EMITTER_AUTO_REMOVE_MINIMUM_DISTANCE, "32");
        properties.setProperty(EMITTER_AUTO_REMOVE_ATTENUATION_DISTANCE, "16");
        properties.setProperty(EMITTER_AUTO_REMOVE_ATTENUATION_COEFFICIENT, "0.25");

        // NeoForge native config directory; keeps the existing user-visible properties-file format.
        Path path = FMLPaths.CONFIGDIR.get().resolve(ParticleStorm.MODID + ".properties");
        if (Files.notExists(path)) {
            writeDefaults(path, properties);
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException exception) {
            ParticleStorm.LOGGER.warn("Failed to load ParticleStorm config '{}', using defaults", path, exception);
        }

        debug = Boolean.parseBoolean(properties.getProperty(DEBUG, "false"));
        showEmitterOutline = Boolean.parseBoolean(properties.getProperty(SHOW_EMITTER_OUTLINE, "true"));
        maxTrackersPerEntity = getInt(properties, MAX_TRACKERS_PER_ENTITY, 64, 0, 16384);
        emitterLimit = getInt(properties, EMITTER_LIMIT, 50, 20, 1000);
        fpsThreshold = getInt(properties, FPS_THRESHOLD, 30, 10, 260);
        allowsVanillaParticleWhenReachLimit = Boolean.parseBoolean(properties.getProperty(ALLOWS_VANILLA_PARTICLE_WHEN_REACH_LIMIT, "false"));
        emitterAutoRemoveIntervalTick = getInt(properties, EMITTER_AUTO_REMOVE_INTERVAL_TICK, 1, 1, 1200);
        emitterAutoRemoveMinimumDistance = getInt(properties, EMITTER_AUTO_REMOVE_MINIMUM_DISTANCE, 32, 16, 256);
        emitterAutoRemoveAttenuationDistance = getInt(properties, EMITTER_AUTO_REMOVE_ATTENUATION_DISTANCE, 16, 0, 64);
        emitterAutoRemoveAttenuationCoefficient = getDouble(properties, EMITTER_AUTO_REMOVE_ATTENUATION_COEFFICIENT, 0.25, 0.0, 1.0);
    }

    private static int getInt(Properties properties, String key, int defaultValue, int min, int max) {
        try {
            int value = Integer.parseInt(properties.getProperty(key, Integer.toString(defaultValue)));
            if (value < min || value > max) {
                ParticleStorm.LOGGER.warn("Invalid integer config '{}' = {}, using default {} (allowed range {}-{})", key, value, defaultValue, min, max);
                return defaultValue;
            }
            return value;
        } catch (NumberFormatException exception) {
            ParticleStorm.LOGGER.warn("Invalid integer config '{}', using default {} (allowed range {}-{})", key, defaultValue, min, max);
            return defaultValue;
        }
    }

    private static double getDouble(Properties properties, String key, double defaultValue, double min, double max) {
        try {
            double value = Double.parseDouble(properties.getProperty(key, Double.toString(defaultValue)));
            if (value < min || value > max) {
                ParticleStorm.LOGGER.warn("Invalid numeric config '{}' = {}, using default {} (allowed range {}-{})", key, value, defaultValue, min, max);
                return defaultValue;
            }
            return value;
        } catch (NumberFormatException exception) {
            ParticleStorm.LOGGER.warn("Invalid numeric config '{}', using default {} (allowed range {}-{})", key, defaultValue, min, max);
            return defaultValue;
        }
    }

    private static void writeDefaults(Path path, Properties properties) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                properties.store(writer, "ParticleStorm configuration");
            }
        } catch (IOException exception) {
            ParticleStorm.LOGGER.warn("Failed to create ParticleStorm config '{}'", path, exception);
        }
    }
}

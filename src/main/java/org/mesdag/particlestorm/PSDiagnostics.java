package org.mesdag.particlestorm;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class PSDiagnostics {
    private static final String PREFIX = "[ParticleStorm diag] ";
    private static final Set<String> ONCE = ConcurrentHashMap.newKeySet();
    private static final Map<String, AtomicInteger> COUNTERS = new ConcurrentHashMap<>();

    private PSDiagnostics() {
    }

    public static void clear() {
        ONCE.clear();
        COUNTERS.clear();
    }

    public static void info(String format, Object... args) {
        if (enabled()) {
            ParticleStorm.LOGGER.info(PREFIX + format, args);
        }
    }

    public static void warn(String format, Object... args) {
        if (enabled()) {
            ParticleStorm.LOGGER.warn(PREFIX + format, args);
        }
    }

    public static void error(String format, Object... args) {
        if (enabled()) {
            ParticleStorm.LOGGER.error(PREFIX + format, args);
        }
    }

    public static void infoOnce(String key, String format, Object... args) {
        if (enabled() && ONCE.add("info:" + key)) {
            info(format, args);
        }
    }

    public static void warnOnce(String key, String format, Object... args) {
        if (enabled() && ONCE.add("warn:" + key)) {
            warn(format, args);
        }
    }

    public static void infoFirstN(String key, int limit, String format, Object... args) {
        if (shouldLog(key, limit)) {
            info(format, args);
        }
    }

    public static void warnFirstN(String key, int limit, String format, Object... args) {
        if (shouldLog(key, limit)) {
            warn(format, args);
        }
    }

    private static boolean shouldLog(String key, int limit) {
        return enabled() && COUNTERS.computeIfAbsent(key, ignored -> new AtomicInteger()).getAndIncrement() < limit;
    }

    private static boolean enabled() {
        return PSClientConfigs.debug;
    }
}

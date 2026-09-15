package org.mesdag.particlestorm;

import it.unimi.dsi.fastutil.objects.ObjectBooleanPair;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.particle.attach.EmitterAttachHandler;
import org.mesdag.particlestorm.particle.attach.WithBlockParticleEmitter;

import java.util.List;

public final class PSClientConfigs {
    private static ModConfigSpec.BooleanValue SHOW_EMITTER_OUTLINE;
    private static ModConfigSpec.IntValue MAX_TRACKERS_PER_ENTITY;

    private static ModConfigSpec.IntValue EMITTER_LIMIT;
    private static ModConfigSpec.IntValue FPS_THRESHOLD;
    private static ModConfigSpec.BooleanValue ALLOWS_VANILLA_PARTICLE_WHEN_REACH_LIMIT;
    private static ModConfigSpec.IntValue EMITTER_AUTO_REMOVE_INTERVAL_TICK;
    private static ModConfigSpec.IntValue EMITTER_AUTO_REMOVE_MINIMUM_DISTANCE;
    private static ModConfigSpec.IntValue EMITTER_AUTO_REMOVE_ATTENUATION_DISTANCE;
    private static ModConfigSpec.DoubleValue EMITTER_AUTO_REMOVE_ATTENUATION_COEFFICIENT;

    public static boolean showEmitterOutline = true;
    public static int maxTrackersPerEntity = 64;

    public static int emitterLimit = 50;
    public static int fpsThreshold = 30;
    public static boolean allowsVanillaParticleWhenReachLimit = false;
    public static int emitterAutoRemoveIntervalTick = 1;
    public static int emitterAutoRemoveMinimumDistance = 32;
    public static int emitterAutoRemoveAttenuationDistance = 16;
    public static double emitterAutoRemoveAttenuationCoefficient = 0.25;

    public static void onLoad() {
        showEmitterOutline = SHOW_EMITTER_OUTLINE.get();
        maxTrackersPerEntity = MAX_TRACKERS_PER_ENTITY.get();

        emitterLimit = EMITTER_LIMIT.get();
        fpsThreshold = FPS_THRESHOLD.get();
        allowsVanillaParticleWhenReachLimit = ALLOWS_VANILLA_PARTICLE_WHEN_REACH_LIMIT.get();
        emitterAutoRemoveIntervalTick = EMITTER_AUTO_REMOVE_INTERVAL_TICK.get();
        emitterAutoRemoveMinimumDistance = EMITTER_AUTO_REMOVE_MINIMUM_DISTANCE.get();
        emitterAutoRemoveAttenuationDistance = EMITTER_AUTO_REMOVE_ATTENUATION_DISTANCE.get();
        emitterAutoRemoveAttenuationCoefficient = EMITTER_AUTO_REMOVE_ATTENUATION_COEFFICIENT.get();
    }

    public static void register(ModContainer container) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("Normal");
        SHOW_EMITTER_OUTLINE = builder.define("showEmitterOutline", true);
        MAX_TRACKERS_PER_ENTITY = builder.defineInRange("maxTrackersPerEntity", 64, 0, 16384);
        builder.pop();

        builder.push("Attach");
        EMITTER_LIMIT = builder.defineInRange("emitterLimit", 50, 20, 1000);
        FPS_THRESHOLD = builder.defineInRange("fpsThreshold", 30, 10, 260);
        ALLOWS_VANILLA_PARTICLE_WHEN_REACH_LIMIT = builder.define("allowsVanillaParticleWhenReachLimit", false);
        EMITTER_AUTO_REMOVE_INTERVAL_TICK = builder.defineInRange("emitterAutoRemoveIntervalTick", 1, 1, 1200);
        EMITTER_AUTO_REMOVE_MINIMUM_DISTANCE = builder.defineInRange("minimumEmitterAutoRemoveDistance", 32, 16, 256);
        EMITTER_AUTO_REMOVE_ATTENUATION_DISTANCE = builder.defineInRange("minimumEmitterAutoRemoveAttenuationDistance", 16, 0, 64);
        EMITTER_AUTO_REMOVE_ATTENUATION_COEFFICIENT = builder.defineInRange("minimumEmitterAutoRemoveAttenuationCoefficient", 0.25, 0, 1);
        builder.pop();

        container.registerConfig(ModConfig.Type.COMMON, builder.build());
    }

    public static class ParticleConfig {
        private boolean enabled = true;
        public @Nullable ResourceLocation particle;
        private @Nullable List<EmitterAttachHandler.AttachData> associated;
        public boolean failed = false;

        private final String configPath;
        private final @Nullable Runnable onLoadCallback;
        private final ModConfigSpec.BooleanValue ENABLE;
        private final ModConfigSpec.ConfigValue<String> PARTICLE;

        public ParticleConfig(ModConfigSpec.Builder builder, String configPath, String particlePath) {
            this(builder, configPath, particlePath, true, null);
        }

        public ParticleConfig(ModConfigSpec.Builder builder, String configPath, String particlePath, @Nullable Runnable onLoadCallback) {
            this(builder, configPath, particlePath, true, onLoadCallback);
        }

        public ParticleConfig(ModConfigSpec.Builder builder, String configPath, String particlePath, boolean enabled, @Nullable Runnable onLoadCallback) {
            this.configPath = configPath;
            this.onLoadCallback = onLoadCallback;
            this.ENABLE = builder.define(configPath, enabled);
            this.PARTICLE = builder.define(configPath + "Particle", "tdp:" + particlePath);
        }

        public boolean isEnabled() {
            return enabled && !failed;
        }

        public boolean enable(boolean enable) {
            if (enable) {
                if (!isEnabled()) {
                    ENABLE.set(true);
                    return true;
                }
            } else if (isEnabled()) {
                ENABLE.set(false);
                return true;
            }
            return false;
        }

        public void onLoad() {
            this.enabled = ENABLE.get();
            this.particle = ResourceLocation.tryParse(PARTICLE.get());
            updateAssociated();
            if (onLoadCallback != null) {
                Minecraft.getInstance().execute(onLoadCallback);
            }
        }

        public void initAssociated(List<EmitterAttachHandler.AttachData> associated) {
            this.associated = associated;
            updateAssociated();
        }

        private void updateAssociated() {
            if (associated == null) return;
            boolean disabled = !enabled;
            for (EmitterAttachHandler.AttachData data : associated) {
                data.disabled = disabled;
            }
            if (particle == null) return;
            Minecraft.getInstance().execute(() -> {
                for (ObjectBooleanPair<WithBlockParticleEmitter> pair : EmitterAttachHandler.attachedToBlockEmitters.values()) {
                    WithBlockParticleEmitter emitter = pair.left();
                    if (particle.equals(emitter.particleId)) {
                        emitter.remove();
                    }
                }
            });
        }

        public void markFailed() {
            this.failed = true;
            ParticleStorm.LOGGER.warn("Error get {} particle", configPath);
        }

        @Override
        public String toString() {
            return "ParticleConfig{" +
                    "configPath='" + configPath + '\'' +
                    '}';
        }
    }
}

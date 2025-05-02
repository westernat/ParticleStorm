package org.mesdag.particlestorm;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class PSClientConfigs {
    private static ModConfigSpec.BooleanValue SHOW_EMITTER_OUTLINE;

    public static boolean showEmitterOutline = true;

    public static void onLoad() {
        showEmitterOutline = SHOW_EMITTER_OUTLINE.get();
    }

    public static void register(ModContainer container) {
        ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
        SHOW_EMITTER_OUTLINE = BUILDER.define("showEmitterOutline", true);
        container.registerConfig(ModConfig.Type.COMMON, BUILDER.build());
    }
}

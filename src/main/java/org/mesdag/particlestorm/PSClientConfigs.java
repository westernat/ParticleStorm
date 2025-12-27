package org.mesdag.particlestorm;


import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class PSClientConfigs {
    private static ForgeConfigSpec.BooleanValue SHOW_EMITTER_OUTLINE;

    public static boolean showEmitterOutline = true;

    public static void onLoad() {
        showEmitterOutline = SHOW_EMITTER_OUTLINE.get();
    }

    public static void register(ModLoadingContext context) {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
        SHOW_EMITTER_OUTLINE = BUILDER.define("showEmitterOutline", true);
        context.registerConfig(ModConfig.Type.COMMON, BUILDER.build());
    }
}

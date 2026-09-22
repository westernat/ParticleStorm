package org.mesdag.particlestorm;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.mesdag.particlestorm.particle.attach.EmitterAttachHandler;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;

/**
 * NeoForge game-bus (NeoForge.EVENT_BUS) client events. Kept separate from
 * {@link PSGameClient} because mod-bus and game-bus subscribers cannot share one class.
 */
@EventBusSubscriber(modid = ParticleStorm.MODID, value = Dist.CLIENT)
public final class PSClientEvents {
    private PSClientEvents() {
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Pre event) {
        PSGameClient.tick();
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PSGameClient.LOADER.removeAll();
        EmitterAttachHandler.clearEmitters();
        if (ParticleStorm.GECKOLIB_LOADED) {
            GeckoLibHelper.clearReloadCallbacks();
        }
    }
}

package org.mesdag.particlestorm.integration.geckolib;

import net.neoforged.fml.ModList;
import org.mesdag.particlestorm.mixinauxi.IParticleKeyframeData;
import software.bernie.geckolib.animation.keyframe.event.ParticleKeyframeEvent;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

public final class GeckoLibHelper {
    private static final double[] ZERO = new double[3];
    private static Boolean isLoaded;

    public static boolean isLoaded() {
        if (isLoaded == null) {
            isLoaded = ModList.get().isLoaded("geckolib");
        }
        return isLoaded;
    }

    public static double[] getLocatorOffset(Object o) {
        if (isLoaded() && o instanceof LocatorValue value) {
            if (value.locatorClass() == null) {
                return value.values();
            }
            return value.locatorClass().offset();
        }
        return ZERO;
    }

    public static double[] getLocatorRotation(Object o) {
        if (isLoaded() && o instanceof LocatorValue value) {
            if (value.locatorClass() == null) {
                return ZERO;
            }
            return value.locatorClass().rotation();
        }
        return ZERO;
    }

    public static boolean processParticleEffect(Object o) {
        if (isLoaded() && o instanceof ParticleKeyframeEvent<?> event) {
            IParticleKeyframeData iData = (IParticleKeyframeData) event.getKeyframeData();
            // todo
            return false;
        }
        return true;
    }
}

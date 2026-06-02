package org.mesdag.particlestorm.mixin.integration.geckolib;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import org.mesdag.particlestorm.mixed.IAnimatableInstanceCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.util.Map;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache", remap = false)
public abstract class AnimatableInstanceCacheMixin implements IAnimatableInstanceCache {
    @Unique
    private Object2IntMap<LocatorValue> particlestorm$cachedId;
    @Unique
    private Map<LocatorValue, GeckoLibHelper.LocatorState> particlestorm$transform;

    @Override
    public Object2IntMap<LocatorValue> particlestorm$getCachedId() {
        if (particlestorm$cachedId == null) {
            this.particlestorm$cachedId = new Object2IntOpenHashMap<>();
            particlestorm$cachedId.defaultReturnValue(-1);
        }
        return particlestorm$cachedId;
    }

    @Override
    public GeckoLibHelper.LocatorState particlestorm$getLocatorState(LocatorValue locator) {
        if (particlestorm$transform == null) {
            this.particlestorm$transform = new Object2ObjectOpenHashMap<>();
        }
        return particlestorm$transform.computeIfAbsent(locator, b -> new GeckoLibHelper.LocatorState());
    }
}

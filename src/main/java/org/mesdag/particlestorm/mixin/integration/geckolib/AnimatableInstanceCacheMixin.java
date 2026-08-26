package org.mesdag.particlestorm.mixin.integration.geckolib;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import org.mesdag.particlestorm.mixed.IPSAnimatableInstanceCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.util.Map;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.animatable.instance.AnimatableInstanceCache", remap = false)
public abstract class AnimatableInstanceCacheMixin implements IPSAnimatableInstanceCache {
    @Unique
    private Object2ObjectMap<LocatorValue, IntList> particlestorm$cachedId;
    @Unique
    private Map<LocatorValue, GeckoLibHelper.LocatorState> particlestorm$transform;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void addRunner(CallbackInfo ci) {
        GeckoLibHelper.addRunner(() -> {
            if (particlestorm$cachedId != null) particlestorm$cachedId.clear();
            if (particlestorm$transform != null) particlestorm$transform.clear();
        });
    }

    @Override
    public Object2ObjectMap<LocatorValue, IntList> particlestorm$getCachedId() {
        if (particlestorm$cachedId == null) {
            this.particlestorm$cachedId = new Object2ObjectOpenHashMap<>();
        }
        return particlestorm$cachedId;
    }

    @Override
    public void particlestorm$createLocatorState(LocatorValue locator) {
        if (particlestorm$transform == null) {
            this.particlestorm$transform = new Object2ObjectOpenHashMap<>();
        }
        GeckoLibHelper.LocatorState state = new GeckoLibHelper.LocatorState();
        state.init(locator);
        particlestorm$transform.put(locator, state);
    }

    @Override
    public @Nullable GeckoLibHelper.LocatorState particlestorm$getLocatorState(LocatorValue locator) {
        if (particlestorm$transform == null) {
            this.particlestorm$transform = new Object2ObjectOpenHashMap<>();
        }
        return particlestorm$transform.get(locator);
    }
}

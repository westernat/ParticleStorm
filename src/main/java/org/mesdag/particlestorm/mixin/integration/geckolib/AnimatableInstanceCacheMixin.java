package org.mesdag.particlestorm.mixin.integration.geckolib;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.joml.Vector3f;
import org.mesdag.particlestorm.mixed.IAnimatableInstanceCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.util.Map;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.animatable.instance.AnimatableInstanceCache", remap = false)
public abstract class AnimatableInstanceCacheMixin implements IAnimatableInstanceCache {
    @Unique
    private Object2IntMap<LocatorValue> particlestorm$cachedId;
    @Unique
    private Map<GeoBone, Vector3f[]> particlestorm$transform;

    @Override
    public Object2IntMap<LocatorValue> particlestorm$getCachedId() {
        if (particlestorm$cachedId == null) {
            this.particlestorm$cachedId = new Object2IntOpenHashMap<>();
            particlestorm$cachedId.defaultReturnValue(-1);
        }
        return particlestorm$cachedId;
    }

    @Override
    public Vector3f[] particlestorm$getTransform(GeoBone bone) {
        if (particlestorm$transform == null) {
            this.particlestorm$transform = new Object2ObjectOpenHashMap<>();
        }
        return particlestorm$transform.computeIfAbsent(bone, b -> new Vector3f[]{new Vector3f(), new Vector3f()});
    }
}

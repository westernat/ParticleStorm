package org.mesdag.particlestorm.mixin.integration.geckolib;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.joml.Vector3f;
import org.mesdag.particlestorm.mixed.IAnimatableInstanceCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.animatable.instance.AnimatableInstanceCache", remap = false)
public class AnimatableInstanceCacheMixin implements IAnimatableInstanceCache {
    @Unique
    private Object2IntMap<LocatorValue> particlestorm$cachedId;
    @Unique
    private Vector3f particlestorm$position;
    @Unique
    private Vector3f particlestorm$rotation;
    @Unique
    private Vector3f particlestorm$scale;

    @Override
    public Object2IntMap<LocatorValue> particlestorm$getCachedId() {
        if (particlestorm$cachedId == null) {
            this.particlestorm$cachedId = new Object2IntOpenHashMap<>();
            particlestorm$cachedId.defaultReturnValue(-1);
        }
        return particlestorm$cachedId;
    }

    @Override
    public Vector3f particlestorm$getPosition() {
        if (particlestorm$position == null) {
            this.particlestorm$position = new Vector3f();
        }
        return particlestorm$position;
    }

    @Override
    public Vector3f particlestorm$getRotation() {
        if (particlestorm$rotation == null) {
            this.particlestorm$rotation = new Vector3f();
        }
        return particlestorm$rotation;
    }

//    @Override
//    public Vector3f particlestorm$getScale() {
//        if (particlestorm$scale == null) {
//            this.particlestorm$scale = new Vector3f();
//        }
//        return particlestorm$scale;
//    }
}

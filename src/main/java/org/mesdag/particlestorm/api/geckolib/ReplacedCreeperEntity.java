package org.mesdag.particlestorm.api.geckolib;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ReplacedCreeperEntity implements ParticleStormGeoReplacedEntity {
    public static final ReplacedCreeperEntity INSTANCE = new ReplacedCreeperEntity();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Entity currentEntity;

    private ReplacedCreeperEntity() {}

    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericWalkIdleController(this));
    }

    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public EntityType<?> getReplacingEntityType() {
        return EntityType.CREEPER;
    }

    @Override
    public Entity getCurrentEntity() {
        return currentEntity;
    }

    @Override
    public void setCurrentEntity(@Nullable Entity entity) {
        this.currentEntity = entity;
    }
}

package org.mesdag.particlestorm.api.geckolib;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ReplacedCreeperEntity implements GeoWithCurrentEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private Entity currentEntity;

    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController[]{DefaultAnimations.genericWalkIdleController(this)});
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
    public void setCurrentEntity(Entity entity) {
        this.currentEntity = entity;
    }
}

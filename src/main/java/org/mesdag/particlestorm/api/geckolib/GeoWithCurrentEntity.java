package org.mesdag.particlestorm.api.geckolib;

import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoReplacedEntity;

public interface GeoWithCurrentEntity extends GeoReplacedEntity {
    Entity getCurrentEntity();

    void setCurrentEntity(Entity entity);
}

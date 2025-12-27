package org.mesdag.particlestorm.api.geckolib;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoReplacedEntity;

public interface GeoWithCurrentEntity extends GeoReplacedEntity {
    @Nullable Entity getCurrentEntity();

    void setCurrentEntity(Entity entity);
}

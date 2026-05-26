package org.mesdag.particlestorm.mixin.integration.geckolib;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.animatable.GeoReplacedEntity")
public interface GeoReplacedEntityMixin {

}

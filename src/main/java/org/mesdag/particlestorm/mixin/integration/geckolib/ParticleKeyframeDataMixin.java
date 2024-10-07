package org.mesdag.particlestorm.mixin.integration.geckolib;

import org.mesdag.particlestorm.mixinauxi.IParticleKeyframeData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.animation.keyframe.event.data.ParticleKeyframeData", remap = false)
public abstract class ParticleKeyframeDataMixin implements IParticleKeyframeData {

}

package org.mesdag.particlestorm.mixin;

import net.minecraft.client.particle.Particle;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Particle.class)
public class ParticleMixin {
    @Unique
    private boolean particlestorm$removalCounted = false;

    @Inject(method = "remove", at = @At("HEAD"))
    private void particlestorm$countRemoval(CallbackInfo ci) {
        if (particlestorm$removalCounted) return;
        particlestorm$removalCounted = true;
        if (this instanceof IMolangParticleInstance instance && instance.getEmitter() != null) {
            instance.getEmitter().onRemoved();
        }
    }
}

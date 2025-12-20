package org.mesdag.particlestorm.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import org.mesdag.particlestorm.mixed.IParticleEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    @Final
    public ParticleEngine particleEngine;

    @Inject(method = "onResourceLoadFinished", at = @At("TAIL"))
    private void onLoaded(@Coerce Object gameLoadCookie, CallbackInfo ci) {
        IParticleEngine.of(particleEngine).particlestorm$bindSprites();
    }
}

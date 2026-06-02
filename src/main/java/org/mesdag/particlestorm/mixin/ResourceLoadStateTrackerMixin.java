package org.mesdag.particlestorm.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ResourceLoadStateTracker;
import org.mesdag.particlestorm.mixed.IParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResourceLoadStateTracker.class)
public abstract class ResourceLoadStateTrackerMixin {
    @Inject(method = "finishReload", at = @At("TAIL"))
    private void end(CallbackInfo ci) {
        IParticleEngine.of(Minecraft.getInstance().particleEngine).particlestorm$bindSprites();
    }
}

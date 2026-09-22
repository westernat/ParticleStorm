package org.mesdag.particlestorm.mixin;

import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {
    @Inject(method = "getGameInformation", at = @At("RETURN"), cancellable = true)
    private void particlestorm$appendCounts(CallbackInfoReturnable<List<String>> cir) {
        List<String> lines = new ArrayList<>(cir.getReturnValue());
        lines.add("MolangParticle: " + MolangParticleEngine.INSTANCE.totalParticleCount()
                + ". ParticleEmitter: " + MolangParticleEngine.INSTANCE.totalEmitterCount());
        cir.setReturnValue(lines);
    }
}

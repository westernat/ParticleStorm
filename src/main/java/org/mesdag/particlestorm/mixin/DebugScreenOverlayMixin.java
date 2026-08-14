package org.mesdag.particlestorm.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {
    @ModifyExpressionValue(method = "getGameInformation", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList([Ljava/lang/Object;)Ljava/util/ArrayList;"))
    private ArrayList<String> modifyParticleCount(ArrayList<String> original) {
        original.add(
                "MolangParticle: " + MolangParticleEngine.INSTANCE.totalParticleCount() +
                        ". ParticleEmitter: " + MolangParticleEngine.INSTANCE.totalEmitterCount()
        );
        return original;
    }
}

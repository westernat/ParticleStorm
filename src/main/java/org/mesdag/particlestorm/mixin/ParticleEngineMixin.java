package org.mesdag.particlestorm.mixin;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.mesdag.particlestorm.PSGameClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Shadow @Final @Mutable
    private static List<ParticleRenderType> RENDER_ORDER;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void particlestorm$registerRenderTypes(CallbackInfo ci) {
        List<ParticleRenderType> types = new ArrayList<>(RENDER_ORDER);
        types.add(PSGameClient.PARTICLE_BLEND);
        types.add(PSGameClient.PARTICLE_ADD);
        RENDER_ORDER = List.copyOf(types);
    }
}

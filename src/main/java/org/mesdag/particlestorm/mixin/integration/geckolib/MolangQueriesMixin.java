package org.mesdag.particlestorm.mixin.integration.geckolib;

import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static software.bernie.geckolib.loading.math.MolangQueries.setActorVariable;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.loading.math.MolangQueries", remap = false)
public abstract class MolangQueriesMixin {
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void particleQueries(CallbackInfo ci) {
        setActorVariable("query.total_emitter_count", actor -> MolangParticleEngine.INSTANCE.totalEmitterCount());
        setActorVariable("query.total_particle_count", actor -> MolangParticleEngine.INSTANCE.totalParticleCount());
    }
}

package org.mesdag.particlestorm.mixin.integration.geckolib;

import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.data.molang.compiler.MolangQueries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.geckolib.loading.math.MolangQueries.setActorVariable;

@Pseudo
@Mixin(targets = "com.geckolib.loading.math.MolangQueries", remap = false)
public abstract class MolangQueriesMixin {
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void particlestorm$particleQueries(CallbackInfo ci) {
        setActorVariable("query.total_emitter_count", actor -> PSGameClient.LOADER.totalEmitterCount());
        setActorVariable("query.total_particle_count", actor -> MolangQueries.totalParticleCount());
    }
}

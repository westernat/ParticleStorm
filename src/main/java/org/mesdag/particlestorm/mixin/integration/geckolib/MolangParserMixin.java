package org.mesdag.particlestorm.mixin.integration.geckolib;

import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.mesdag.particlestorm.data.molang.compiler.MolangQueries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.DoubleSupplier;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.core.molang.MolangParser", remap = false)
public abstract class MolangParserMixin {
    @Shadow
    public abstract void setValue(String name, DoubleSupplier value);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void particleQueries(CallbackInfo ci) {
        setValue("query.total_emitter_count", () -> MolangParticleEngine.INSTANCE.totalEmitterCount());
        setValue("query.total_particle_count", MolangQueries::totalParticleCount);
    }
}

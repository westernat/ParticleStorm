package org.mesdag.particlestorm.mixin.integration.geckolib;

import net.minecraft.client.Minecraft;
import org.mesdag.particlestorm.GameClient;
import org.mesdag.particlestorm.mixin.ParticleEngineAccessor;
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
        setActorVariable("query.total_emitter_count", actor -> GameClient.LOADER.totalEmitterCount());
        setActorVariable("query.total_particle_count", actor -> {
            int sum = 0;
            for (Integer value : ((ParticleEngineAccessor) Minecraft.getInstance().particleEngine).trackedParticleCounts().values()) {
                sum += value;
            }
            return sum;
        });
    }
}

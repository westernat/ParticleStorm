package org.mesdag.particlestorm.mixin.integration.geckolib;

import com.eliotlash.mclib.math.Variable;
import net.minecraft.client.Minecraft;
import org.mesdag.particlestorm.PSGameClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.core.molang.LazyVariable;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.core.molang.MolangParser", remap = false)
public abstract class MolangQueriesMixin {
    @Shadow
    public abstract void register(Variable variable);

    @Inject(method = "registerAdditionalVariables()V", at = @At("TAIL"))
    private void particleQueries(CallbackInfo ci) {
        register(new LazyVariable("query.total_emitter_count", () -> (double) PSGameClient.LOADER.totalEmitterCount()));
        register(new LazyVariable("query.total_particle_count", () -> {
            int sum = 0;
            for (Integer value : Minecraft.getInstance().particleEngine.trackedParticleCounts.values()) {
                sum += value;
            }
            return sum;
        }));
    }
}

package org.mesdag.particlestorm.mixin.integration.geckolib;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;
import software.bernie.geckolib.loading.math.MolangQueries;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.loading.math.MolangQueries")
public interface MolangQueriesAccessor {
    @Invoker
    static MolangQueries.Actor<?> callGetActor() {throw new UnsupportedOperationException();}
}

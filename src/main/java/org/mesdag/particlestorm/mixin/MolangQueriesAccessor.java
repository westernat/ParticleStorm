package org.mesdag.particlestorm.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import software.bernie.geckolib.loading.math.MolangQueries;

@Mixin(MolangQueries.class)
public interface MolangQueriesAccessor {
    @Invoker
    static MolangQueries.Actor<?> callGetActor() {throw new UnsupportedOperationException();}
}

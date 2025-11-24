package org.mesdag.particlestorm;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.mesdag.particlestorm.api.RegisterCustomParticleTypeEvent;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

@EventBusSubscriber(modid = ParticleStorm.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PSModClient {
    @SubscribeEvent
    public static void registerCustomParticleType(RegisterCustomParticleTypeEvent event) {
        event.registerWithSprites(ParticleStorm.MOLANG, (emitter, particlePreset, level, x, y, z, sprites) ->
                new MolangParticleInstance(particlePreset, level, x, y, z, sprites)
        );
    }
}

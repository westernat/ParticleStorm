package org.mesdag.particlestorm;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.mesdag.particlestorm.data.component.*;
import org.mesdag.particlestorm.particle.MolangParticleLoader;
import org.mesdag.particlestorm.particle.ParticleEmitterRenderer;

@EventBusSubscriber(modid = ParticleStorm.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GameClient {
    public static final MolangParticleLoader LOADER = new MolangParticleLoader();

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ParticleStorm.PARTICLE_EMITTER.get(), ParticleEmitterRenderer::new);
    }

    @SubscribeEvent
    public static void reload(RegisterClientReloadListenersEvent event) {
        IComponent.register("emitter_local_space", EmitterLocalSpace.CODEC);
        IComponent.register(EmitterInitialization.ID, EmitterInitialization.CODEC);

        IComponent.register("emitter_rate_instant", EmitterRate.Instant.CODEC);
        IComponent.register("emitter_rate_steady", EmitterRate.Steady.CODEC);
        IComponent.register("emitter_rate_manual", EmitterRate.Manual.CODEC);

        IComponent.register("emitter_lifetime_looping", EmitterLifetime.Looping.CODEC);
        IComponent.register("emitter_lifetime_once", EmitterLifetime.Once.CODEC);
        IComponent.register("emitter_lifetime_expression", EmitterLifetime.Expression.CODEC);
        // todo emitter_lifetime_events

        IComponent.register("emitter_shape_point", EmitterShape.Point.CODEC);
        IComponent.register("emitter_shape_sphere", EmitterShape.Sphere.CODEC);
        IComponent.register("emitter_shape_box", EmitterShape.Box.CODEC);
        IComponent.register("emitter_shape_custom", EmitterShape.Custom.CODEC);
        IComponent.register("emitter_shape_entity_aabb", EmitterShape.EntityAABB.CODEC);
        IComponent.register("emitter_shape_disc", EmitterShape.Disc.CODEC);

        IComponent.register("particle_initial_speed", ParticleInitialSpeed.CODEC);
        IComponent.register("particle_initial_spin", ParticleInitialSpin.CODEC);
        IComponent.register(ParticleInitialization.ID, ParticleInitialization.CODEC);

        IComponent.register("particle_motion_dynamic", ParticleMotionDynamic.CODEC);
        IComponent.register("particle_motion_parametric", ParticleMotionParametric.CODEC);
        IComponent.register("particle_motion_collision", ParticleMotionCollision.CODEC);

        IComponent.register(ParticleAppearanceBillboard.ID, ParticleAppearanceBillboard.CODEC);
        IComponent.register("particle_appearance_tinting", ParticleAppearanceTinting.CODEC);
        IComponent.register(ParticleAppearanceLighting.ID, ParticleAppearanceLighting.CODEC);

        IComponent.register(ParticleLifetimeExpression.ID, ParticleLifetimeExpression.CODEC);
        // todo particle_lifetime_events
        IComponent.register("particle_kill_plane", ParticleLifetimeKillPlane.CODEC);
        IComponent.register("particle_expire_if_in_blocks", ParticleExpireIfInBlocks.CODEC);
        IComponent.register("particle_expire_if_not_in_blocks", ParticleExpireIfNotInBlocks.CODEC);

        event.registerReloadListener(LOADER);
    }
}
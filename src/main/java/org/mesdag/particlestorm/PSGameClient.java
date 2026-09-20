package org.mesdag.particlestorm;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import org.mesdag.particlestorm.api.IComponent;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.api.RegisterCustomComponentEvent;
import org.mesdag.particlestorm.api.RegisterCustomEmitterTypeEvent;
import org.mesdag.particlestorm.api.RegisterCustomEventNodeEvent;
import org.mesdag.particlestorm.api.RegisterCustomParticleTypeEvent;
import org.mesdag.particlestorm.compat.iris.IrisParticlePipelines;
import org.mesdag.particlestorm.data.component.*;
import org.mesdag.particlestorm.data.event.*;
import org.mesdag.particlestorm.network.EmitterAttachPacketS2C;
import org.mesdag.particlestorm.network.EmitterCreationPacketS2C;
import org.mesdag.particlestorm.network.EmitterRemovalPacket;
import org.mesdag.particlestorm.network.EmitterSynchronizePacket;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.mesdag.particlestorm.particle.MolangParticleInstance;
import org.mesdag.particlestorm.particle.ParticleEmitter;
import org.mesdag.particlestorm.particle.attach.EmitterAttachHandler;

/**
 * NeoForge client entry/state holder.
 * Registered on the mod event bus (Dist.CLIENT); game-bus client events live in {@link PSClientEvents}.
 * The class name and static state are kept so the particle package keeps its existing static references.
 */
@EventBusSubscriber(modid = ParticleStorm.MODID, value = Dist.CLIENT)
public final class PSGameClient {
    public static final MolangParticleEngine LOADER = MolangParticleEngine.INSTANCE;
    public static SingleQuadParticle.Layer PARTICLE_ADD;
    public static SingleQuadParticle.Layer PARTICLE_BLEND;

    private PSGameClient() {
    }

    @SubscribeEvent
    public static void registerRenderPipelines(RegisterRenderPipelinesEvent event) {
        RenderPipeline additivePipeline = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                .withLocation(ParticleStorm.asResource("pipeline/additive_particle"))
                .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                .withCull(true)
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                .build();
        PARTICLE_ADD = new SingleQuadParticle.Layer(
                true,
                TextureAtlas.LOCATION_PARTICLES,
                additivePipeline
        );
        event.registerPipeline(additivePipeline);

        RenderPipeline blendPipeline = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                .withLocation(ParticleStorm.asResource("pipeline/blend_particle"))
                .withFragmentShader(ParticleStorm.asResource("core/particle_no_discard"))
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withCull(true)
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                .build();
        PARTICLE_BLEND = new SingleQuadParticle.Layer(
                true,
                TextureAtlas.LOCATION_PARTICLES,
                blendPipeline
        );
        event.registerPipeline(blendPipeline);
    }

    @SubscribeEvent
    public static void registerParticleProvider(RegisterParticleProvidersEvent event) {
        event.registerSpecial(ParticleStorm.MOLANG, new MolangParticleInstance.Provider());
    }

    @SubscribeEvent
    public static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        // Client-side payload handlers. Registered per side on the default main thread:
        // no explicit HandlerThread.NETWORK and no enqueueWork.
        event.register(EmitterCreationPacketS2C.TYPE, EmitterCreationPacketS2C::handleClient);
        event.register(EmitterAttachPacketS2C.TYPE, EmitterAttachPacketS2C::handleClient);
        event.register(EmitterRemovalPacket.TYPE, EmitterRemovalPacket::handleClient);
        event.register(EmitterSynchronizePacket.TYPE, EmitterSynchronizePacket::handleClient);
    }

    @SubscribeEvent
    public static void addReloadListeners(AddClientReloadListenersEvent event) {
        RegisterCustomEmitterTypeEvent.postEvent();
        event.addListener(MolangParticleEngine.RELOADER_ID, LOADER);
    }

    @SubscribeEvent
    public static void fmlClientSetup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("iris")) {
            IrisParticlePipelines.register();
        }
        // Mirrors the Fabric client initializer order: register codecs/event nodes/defaults once
        // during client setup, before the reload listener starts parsing particle definitions.
        registerComponents();
        registerEventNodes();
        RegisterCustomParticleTypeEvent.registerDefaults();
        EmitterAttachHandler.postEvent();
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer == null) {
            LOADER.removeAll();
        } else if (!minecraft.isPaused() && localPlayer.level().tickRateManager().runsNormally()) {
            LOADER.tick(localPlayer);
            if (PSClientConfigs.emitterAutoRemoveIntervalTick <= 1 || localPlayer.level().getGameTime() % PSClientConfigs.emitterAutoRemoveIntervalTick == 0) {
                Camera camera = minecraft.gameRenderer.getMainCamera();
                if (camera.isInitialized()) {
                    EmitterAttachHandler.tick(camera);
                }
            }
            collectEmitterGizmos();
        }
    }

    @SubscribeEvent
    public static void registerDebugEntries(RegisterDebugEntriesEvent event) {
        PSDebugEntries.register(event);
    }

    private static void collectEmitterGizmos() {
        if (!PSClientConfigs.showEmitterOutline) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES)) {
            return;
        }

        try {
            for (ParticleEmitter emitter : LOADER.getEmitters()) {
                Vec3 pos = emitter.pos;
                int particleCount = emitter.activeParticleCount;
                int limit = emitter.particleGroup == null ? 0 : emitter.particleGroup.limit();
                int countColor = limit > 0 && particleCount >= limit ? 0xFFFF0000 : 0xFFFFFFFF;

                Gizmos.cuboid(new AABB(pos.subtract(0.5, 0.5, 0.5), pos.add(0.5, 0.5, 0.5)), GizmoStyle.stroke(0xFF00FF00, 2.5F)).persistForMillis(50);
                Gizmos.billboardText(emitter.particleId == null ? "unknown" : emitter.particleId.toString(), pos.add(0.0, 0.5, 0.0), TextGizmo.Style.forColorAndCentered(0xFFFFFFFF).withScale(0.22F)).setAlwaysOnTop().persistForMillis(50);
                Gizmos.billboardText("id: " + emitter.id, pos.add(0.0, 0.3, 0.0), TextGizmo.Style.forColorAndCentered(0xFFFFFFFF).withScale(0.2F)).setAlwaysOnTop().persistForMillis(50);
                Gizmos.billboardText("particles: " + particleCount, pos.add(0.0, 0.1, 0.0), TextGizmo.Style.forColorAndCentered(countColor).withScale(0.2F)).setAlwaysOnTop().persistForMillis(50);
            }
        } catch (IllegalStateException exception) {
            PSDiagnostics.warnOnce("emitter-gizmo-context", "emitter outline skipped because no Gizmo collector is active");
        }
    }

    private static void registerComponents() {
        IComponent.register("emitter_local_space", EmitterLocalSpace.CODEC);
        IComponent.register("emitter_initialization", EmitterInitialization.CODEC);

        IComponent.register("emitter_rate_instant", EmitterRate.Instant.CODEC);
        IComponent.register("emitter_rate_steady", EmitterRate.Steady.CODEC);
        IComponent.register("emitter_rate_manual", EmitterRate.Manual.CODEC);

        IComponent.register("emitter_lifetime_looping", EmitterLifetime.Looping.CODEC);
        IComponent.register("emitter_lifetime_once", EmitterLifetime.Once.CODEC);
        IComponent.register("emitter_lifetime_expression", EmitterLifetime.Expression.CODEC);
        IComponent.register("emitter_lifetime_events", EmitterLifetimeEvents.CODEC);

        IComponent.register("emitter_shape_point", EmitterShape.Point.CODEC);
        IComponent.register("emitter_shape_sphere", EmitterShape.Sphere.CODEC);
        IComponent.register("emitter_shape_box", EmitterShape.Box.CODEC);
        IComponent.register("emitter_shape_entity_aabb", EmitterShape.EntityAABB.CODEC);
        IComponent.register("emitter_shape_disc", EmitterShape.Disc.CODEC);

        IComponent.register("particle_initial_speed", ParticleInitialSpeed.CODEC);
        IComponent.register("particle_initial_spin", ParticleInitialSpin.CODEC);
        IComponent.register(ParticleInitialization.ID, ParticleInitialization.CODEC);

        IComponent.register(ParticleMotionDynamic.ID, ParticleMotionDynamic.CODEC);
        IComponent.register("particle_motion_parametric", ParticleMotionParametric.CODEC);
        IComponent.register(ParticleMotionCollision.ID, ParticleMotionCollision.CODEC);

        IComponent.register(ParticleAppearanceBillboard.ID, ParticleAppearanceBillboard.CODEC);
        IComponent.register("particle_appearance_tinting", ParticleAppearanceTinting.CODEC);
        IComponent.register("particle_appearance_lighting", ParticleAppearanceLighting.CODEC);

        IComponent.register("particle_lifetime_expression", ParticleLifetimeExpression.CODEC);
        IComponent.register(ParticleLifeTimeEvents.ID, ParticleLifeTimeEvents.CODEC);
        IComponent.register("particle_kill_plane", ParticleLifetimeKillPlane.CODEC);
        IComponent.register("particle_expire_if_in_blocks", ParticleExpireIfInBlocks.CODEC);
        IComponent.register("particle_expire_if_not_in_blocks", ParticleExpireIfNotInBlocks.CODEC);

        PSModClient.registerCustomComponent(new RegisterCustomComponentEvent());
    }

    private static void registerEventNodes() {
        IEventNode.register("sequence", EventSequence.CODEC);
        IEventNode.register("weight", EventRandomize.Weight.CODEC);
        IEventNode.register("randomize", EventRandomize.CODEC);
        IEventNode.register("particle_effect", ParticleEffect.CODEC.codec());
        IEventNode.register("sound_effect", SoundEffect.CODEC.codec());
        IEventNode.register("expression", NodeMolangExp.CODEC);
        IEventNode.register("log", EventLog.CODEC);

        PSModClient.registerCustomEventNode(new RegisterCustomEventNodeEvent());
    }
}

package org.mesdag.particlestorm;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.mesdag.particlestorm.api.IComponent;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.api.RegisterCustomComponentEvent;
import org.mesdag.particlestorm.api.RegisterCustomEmitterTypeEvent;
import org.mesdag.particlestorm.api.RegisterCustomEventNodeEvent;
import org.mesdag.particlestorm.api.RegisterCustomParticleTypeEvent;
import org.mesdag.particlestorm.data.component.*;
import org.mesdag.particlestorm.data.event.*;
import org.mesdag.particlestorm.mixin.DebugScreenEntriesAccessor;
import org.mesdag.particlestorm.network.EmitterAttachPacketS2C;
import org.mesdag.particlestorm.network.EmitterCreationPacketS2C;
import org.mesdag.particlestorm.network.EmitterRemovalPacket;
import org.mesdag.particlestorm.network.EmitterSynchronizePacket;
import org.mesdag.particlestorm.particle.attach.EmitterAttachHandler;
import org.mesdag.particlestorm.particle.MolangParticleInstance;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.nio.file.Files;

public final class PSGameClient implements ClientModInitializer {
    public static final MolangParticleEngine LOADER = MolangParticleEngine.INSTANCE;
    private static final Identifier MOLANG_PARTICLE_ENTRY = Identifier.withDefaultNamespace("molang_particle");
    private static final Identifier PARTICLE_EMITTER_ENTRY = Identifier.withDefaultNamespace("particle_emitter");
    private static boolean debugEntriesReady = false;
    public static final SingleQuadParticle.Layer PARTICLE_ADD = new SingleQuadParticle.Layer(
            true,
            TextureAtlas.LOCATION_PARTICLES,
            RenderPipelines.register(RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                    .withLocation(ParticleStorm.asResource("pipeline/additive_particle"))
                    .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                    .withCull(true)
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    .build()
            )
    );
    public static final SingleQuadParticle.Layer PARTICLE_BLEND = new SingleQuadParticle.Layer(
            true,
            TextureAtlas.LOCATION_PARTICLES,
            RenderPipelines.register(RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                    .withLocation(ParticleStorm.asResource("pipeline/blend_particle"))
                    .withFragmentShader(ParticleStorm.asResource("core/particle_no_discard"))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(true)
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                    .build()
            )
    );

    @Override
    public void onInitializeClient() {
        PSClientConfigs.onLoad();
        registerComponents();
        registerEventNodes();
        RegisterCustomParticleTypeEvent.registerDefaults();
        RegisterCustomEmitterTypeEvent.postEvent();
        registerDebugEntries();
        EmitterAttachHandler.postEvent();

        ParticleProviderRegistry.getInstance().register(ParticleStorm.MOLANG, new MolangParticleInstance.Provider());
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(MolangParticleEngine.RELOADER_ID, LOADER);
        ClientTickEvents.START_LEVEL_TICK.register(level -> tick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOADER.removeAll();
            EmitterAttachHandler.clearEmitters();
        });

        ClientPlayNetworking.registerGlobalReceiver(EmitterCreationPacketS2C.TYPE, EmitterCreationPacketS2C::handleClient);
        ClientPlayNetworking.registerGlobalReceiver(EmitterAttachPacketS2C.TYPE, EmitterAttachPacketS2C::handleClient);
        ClientPlayNetworking.registerGlobalReceiver(EmitterRemovalPacket.TYPE, EmitterRemovalPacket::handleClient);
        ClientPlayNetworking.registerGlobalReceiver(EmitterSynchronizePacket.TYPE, EmitterSynchronizePacket::handleClient);
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        enableDebugEntries(minecraft);
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

    private static void registerDebugEntries() {
        if (DebugScreenEntries.getEntry(MOLANG_PARTICLE_ENTRY) != null) return;
        DebugScreenEntriesAccessor.particlestorm$invokeRegister("molang_particle", (DebugScreenEntry) (displayer, level, levelChunk, otherChunk) ->
                displayer.addLine("MolangParticle: " + LOADER.totalParticleCount()));
        DebugScreenEntriesAccessor.particlestorm$invokeRegister("particle_emitter", (DebugScreenEntry) (displayer, level, levelChunk, otherChunk) ->
                displayer.addLine("ParticleEmitter: " + LOADER.totalEmitterCount()));
    }

    /// Enable the two F3 entries by default on a fresh game directory only, so existing debug profile settings are never overwritten.
    private static void enableDebugEntries(Minecraft minecraft) {
        if (debugEntriesReady) return;
        debugEntriesReady = true;
        if (minecraft.debugEntries == null) return;
        if (Files.exists(FabricLoader.getInstance().getGameDir().resolve("debug-profile.json"))) return;
        if (minecraft.debugEntries.getStatus(MOLANG_PARTICLE_ENTRY) == DebugScreenEntryStatus.NEVER) {
            minecraft.debugEntries.setStatus(MOLANG_PARTICLE_ENTRY, DebugScreenEntryStatus.IN_OVERLAY);
            minecraft.debugEntries.setStatus(PARTICLE_EMITTER_ENTRY, DebugScreenEntryStatus.IN_OVERLAY);
        }
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

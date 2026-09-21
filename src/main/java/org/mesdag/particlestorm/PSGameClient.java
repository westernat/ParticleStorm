package org.mesdag.particlestorm;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import org.mesdag.particlestorm.api.IComponent;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.api.RegisterCustomComponentEvent;
import org.mesdag.particlestorm.api.RegisterCustomEmitterTypeEvent;
import org.mesdag.particlestorm.api.RegisterCustomEventNodeEvent;
import org.mesdag.particlestorm.api.RegisterCustomParticleTypeEvent;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
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

public final class PSGameClient implements ClientModInitializer {
    public static final MolangParticleEngine LOADER = MolangParticleEngine.INSTANCE;
    public static final boolean IRIS_LOADED = FabricLoader.getInstance().isModLoaded("iris");

    public static final ParticleRenderType PARTICLE_ADD = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "PARTICLE_ADD";
        }
    };

    public static final ParticleRenderType PARTICLE_BLEND = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            if (!IRIS_LOADED) {
                RenderSystem.setShader(PSGameClient::getParticleNoDiscardShader);
            }
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "PARTICLE_BLEND";
        }
    };

    private static ShaderInstance particleNoDiscard;

    public static ShaderInstance getParticleNoDiscardShader() {
        return particleNoDiscard;
    }

    @Override
    public void onInitializeClient() {
        CoreShaderRegistrationCallback.EVENT.register(context ->
                context.register(ParticleStorm.asResource("particle_no_discard"), DefaultVertexFormat.PARTICLE, shader -> particleNoDiscard = shader)
        );

        PSClientConfigs.onLoad();
        RegisterCustomParticleTypeEvent.registerDefaults();
        ParticleFactoryRegistry.getInstance().register(ParticleStorm.MOLANG, new MolangParticleInstance.Provider());
        registerComponents();
        registerEventNodes();
        RegisterCustomEmitterTypeEvent.postEvent();
        EmitterAttachHandler.postEvent();
        if (ParticleStorm.GECKOLIB_LOADED) {
            GeckoLibHelper.postEvent();
        }

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(LOADER);
        ClientTickEvents.START_CLIENT_TICK.register(client -> tick());
        WorldRenderEvents.AFTER_ENTITIES.register(PSGameClient::renderEmitterOutlines);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOADER.removeAll();
            EmitterAttachHandler.clearEmitters();
            if (ParticleStorm.GECKOLIB_LOADED) {
                GeckoLibHelper.clearReloadCallbacks();
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(EmitterCreationPacketS2C.TYPE, EmitterCreationPacketS2C::handleClient);
        ClientPlayNetworking.registerGlobalReceiver(EmitterAttachPacketS2C.TYPE, EmitterAttachPacketS2C::handleClient);
        ClientPlayNetworking.registerGlobalReceiver(EmitterRemovalPacket.TYPE, EmitterRemovalPacket::handleClient);
        ClientPlayNetworking.registerGlobalReceiver(EmitterSynchronizePacket.TYPE, EmitterSynchronizePacket::handleClient);
    }

    private static void tick() {
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
        }
    }

    private static void renderEmitterOutlines(WorldRenderContext context) {
        if (!PSClientConfigs.showEmitterOutline) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.getEntityRenderDispatcher().shouldRenderHitBoxes()) {
            return;
        }

        PoseStack poseStack = context.matrixStack();
        MultiBufferSource consumers = context.consumers();
        if (poseStack == null || consumers == null) {
            return;
        }

        Vec3 cameraPos = context.camera().getPosition();
        float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(true);
        for (ParticleEmitter emitter : LOADER.getEmitters()) {
            double x = Mth.lerp(partialTick, emitter.posO.x, emitter.getX());
            double y = Mth.lerp(partialTick, emitter.posO.y, emitter.getY());
            double z = Mth.lerp(partialTick, emitter.posO.z, emitter.getZ());
            int particleCount = emitter.activeParticleCount;
            int limit = emitter.particleGroup == null ? 0 : emitter.particleGroup.getLimit();
            int countColor = limit > 0 && particleCount >= limit ? 0xFF0000 : 0xFFFFFF;

            DebugRenderer.renderFloatingText(poseStack, consumers, emitter.particleId == null ? "unknown" : emitter.particleId.toString(), x, y + 0.5, z, 0xFFFFFF);
            DebugRenderer.renderFloatingText(poseStack, consumers, "id: " + emitter.id, x, y + 0.3, z, 0xFFFFFF);
            DebugRenderer.renderFloatingText(poseStack, consumers, "particles: " + particleCount, x, y + 0.1, z, countColor);
            poseStack.pushPose();
            poseStack.translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z);
            LevelRenderer.renderLineBox(poseStack, consumers.getBuffer(RenderType.lines()), -0.5, -0.5, -0.5, 0.5, 0.5, 0.5, 0.0F, 1.0F, 0.0F, 1.0F);
            poseStack.popPose();
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

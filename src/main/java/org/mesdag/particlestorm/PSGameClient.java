package org.mesdag.particlestorm;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.*;
import org.mesdag.particlestorm.api.*;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import org.mesdag.particlestorm.data.component.*;
import org.mesdag.particlestorm.data.event.*;
import org.mesdag.particlestorm.particle.MolangParticleInstance;
import org.mesdag.particlestorm.particle.MolangParticleLoader;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.io.IOException;

@EventBusSubscriber(modid = ParticleStorm.MODID, value = Dist.CLIENT)
public final class PSGameClient {
    public static final MolangParticleLoader LOADER = new MolangParticleLoader();
    public static final ParticleRenderType PARTICLE_ADD = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        public String toString() {
            return "PARTICLE_ADD";
        }
    };
    public static final ParticleRenderType PARTICLE_BLEND = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        public String toString() {
            return "PARTICLE_BLEND";
        }
    };

    private static ShaderInstance particleNoDiscard;

    public static ShaderInstance getParticleNoDiscardShader() {
        return particleNoDiscard;
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ParticleStorm.asResource("particle_no_discard"), DefaultVertexFormat.PARTICLE), instance -> particleNoDiscard = instance);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (ParticleStorm.DEBUG) {
            GeckoLibHelper.registerRenderers(event);
        }
    }

    @SubscribeEvent
    public static void modConfig$Loading(ModConfigEvent.Loading event) {
        if (ParticleStorm.MODID.equals(event.getConfig().getModId())) {
            PSClientConfigs.onLoad();
        }
    }

    @SubscribeEvent
    public static void modConfig$Reloading(ModConfigEvent.Reloading event) {
        if (ParticleStorm.MODID.equals(event.getConfig().getModId())) {
            PSClientConfigs.onLoad();
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer == null) {
            LOADER.removeAll();
        } else if (!minecraft.isPaused() && localPlayer.level().tickRateManager().runsNormally()) {
            LOADER.tick(localPlayer);
        }
    }

    @SubscribeEvent
    public static void renderLevelStage(RenderLevelStageEvent event) {
        if (!PSClientConfigs.showEmitterOutline) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES && minecraft.getEntityRenderDispatcher().shouldRenderHitBoxes()) {
            float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(true);
            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
            for (ParticleEmitter emitter : LOADER.getEmitters()) {
                double x = Mth.lerp(partialTicks, emitter.posO.x, emitter.pos.x);
                double y = Mth.lerp(partialTicks, emitter.posO.y, emitter.pos.y);
                double z = Mth.lerp(partialTicks, emitter.posO.z, emitter.pos.z);
                DebugRenderer.renderFloatingText(poseStack, bufferSource, emitter.particleId.toString(), x, y + 0.5, z, 0xFFFFFF);
                DebugRenderer.renderFloatingText(poseStack, bufferSource, "id: " + emitter.id, x, y + 0.3, z, 0xFFFFFF);
                int maxNum = minecraft.particleEngine.trackedParticleCounts.getInt(emitter.particleGroup);
                DebugRenderer.renderFloatingText(poseStack, bufferSource, "particles: " + maxNum, x, y + 0.1, z, maxNum >= emitter.particleGroup.getLimit() ? 0xFF0000 : 0xFFFFFF);
                Camera camera = event.getCamera();
                double d0 = camera.getPosition().x;
                double d1 = camera.getPosition().y;
                double d2 = camera.getPosition().z;
                poseStack.pushPose();
                poseStack.translate(x - d0, y - d1, z - d2);
                LevelRenderer.renderLineBox(poseStack, bufferSource.getBuffer(RenderType.lines()), -0.5, -0.5, -0.5, 0.5, 0.5, 0.5, 0, 1, 0, 1);
                poseStack.popPose();
            }
        }
    }

    @SubscribeEvent
    public static void reload(RegisterClientReloadListenersEvent event) {
        registerComponents();
        registerEventNodes();
        event.registerReloadListener(LOADER);
    }

    @SubscribeEvent
    public static void registerCustomParticleType(RegisterCustomParticleTypeEvent event) {
        event.registerWithSprites(ParticleStorm.MOLANG, (emitter, particlePreset, level, x, y, z, sprites) ->
                new MolangParticleInstance(particlePreset, level, x, y, z, sprites)
        );
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
        IComponent.register("particle_initialization", ParticleInitialization.CODEC);

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

        ModLoader.postEvent(new RegisterCustomComponentEvent());
    }

    private static void registerEventNodes() {
        IEventNode.register("sequence", EventSequence.CODEC);
        IEventNode.register("weight", EventRandomize.Weight.CODEC);
        IEventNode.register("randomize", EventRandomize.CODEC);
        IEventNode.register("particle_effect", ParticleEffect.CODEC.codec());
        IEventNode.register("sound_effect", SoundEffect.CODEC.codec());
        IEventNode.register("expression", NodeMolangExp.CODEC);
        IEventNode.register("log", EventLog.CODEC);

        ModLoader.postEvent(new RegisterCustomEventNodeEvent());
    }
}

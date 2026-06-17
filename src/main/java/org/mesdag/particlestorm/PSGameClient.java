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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.mesdag.particlestorm.api.*;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import org.mesdag.particlestorm.data.component.*;
import org.mesdag.particlestorm.data.event.*;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.mesdag.particlestorm.particle.MolangParticleInstance;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.io.IOException;
import java.util.Queue;

@Mod.EventBusSubscriber(modid = ParticleStorm.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PSGameClient {
    public static final ParticleRenderType PARTICLE_ADD = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder builder, TextureManager textureManager) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
        }

        public String toString() {
            return "PARTICLE_ADD";
        }
    };
    public static final ParticleRenderType PARTICLE_BLEND = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder builder, TextureManager textureManager) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
        }

        public String toString() {
            return "PARTICLE_BLEND";
        }
    };

    public static boolean isTranslucent(ParticleRenderType type) {
        return type != ParticleRenderType.PARTICLE_SHEET_OPAQUE && type != ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    public static boolean isNotTranslucent(ParticleRenderType type) {
        return type == ParticleRenderType.PARTICLE_SHEET_OPAQUE || type == ParticleRenderType.PARTICLE_SHEET_LIT;
    }

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
    public static void reload(RegisterClientReloadListenersEvent event) {
        registerComponents();
        registerEventNodes();
        event.registerReloadListener(MolangParticleEngine.INSTANCE);
    }

    @SubscribeEvent
    public static void registerCustomParticleType(RegisterCustomParticleTypeEvent event) {
        event.registerWithSprites(ParticleStorm.MOLANG.get(), (emitter, particlePreset, level, x, y, z, sprites) ->
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

        ModLoader.get().postEvent(new RegisterCustomComponentEvent());
    }

    private static void registerEventNodes() {
        IEventNode.register("sequence", EventSequence.CODEC);
        IEventNode.register("weight", EventRandomize.Weight.CODEC);
        IEventNode.register("randomize", EventRandomize.CODEC);
        IEventNode.register("particle_effect", ParticleEffect.CODEC);
        IEventNode.register("sound_effect", SoundEffect.CODEC);
        IEventNode.register("expression", NodeMolangExp.CODEC);
        IEventNode.register("log", EventLog.CODEC);

        ModLoader.get().postEvent(new RegisterCustomEventNodeEvent());
    }

    @SubscribeEvent
    public static void fmlClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (ParticleStorm.GECKOLIB_LOADED) {
                GeckoLibHelper.postEvent();
            }
            RegisterCustomEmitterTypeEvent.postEvent();
        });
    }

    @Mod.EventBusSubscriber(modid = ParticleStorm.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeEvents {
        @SubscribeEvent
        public static void clientNetwork$LoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            MolangParticleEngine.INSTANCE.removeAll();
        }

        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.START) return;
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (player != null && !minecraft.isPaused()) {
                MolangParticleEngine.INSTANCE.tick(minecraft, player);
            }
        }

        @SubscribeEvent
        public static void renderLevelStage(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                Minecraft minecraft = Minecraft.getInstance();
                Camera camera = event.getCamera();
                float partialTick = event.getPartialTick();
                if (PSClientConfigs.showEmitterOutline && minecraft.getEntityRenderDispatcher().shouldRenderHitBoxes()) {
                    PoseStack poseStack = event.getPoseStack();
                    MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
                    double camX = camera.getPosition().x;
                    double camY = camera.getPosition().y;
                    double camZ = camera.getPosition().z;
                    var iterator = MolangParticleEngine.INSTANCE.getEmitters();
                    while (iterator.hasNext()) {
                        ParticleEmitter emitter = iterator.next().getValue();
                        if (emitter.hideOutline) continue;
                        double x = Mth.lerp(partialTick, emitter.posO.x, emitter.getX());
                        double y = Mth.lerp(partialTick, emitter.posO.y, emitter.getY());
                        double z = Mth.lerp(partialTick, emitter.posO.z, emitter.getZ());
                        DebugRenderer.renderFloatingText(poseStack, bufferSource, emitter.particleId.toString(), x, y + 0.5, z, 0xFFFFFF);
                        DebugRenderer.renderFloatingText(poseStack, bufferSource, "id: " + emitter.id, x, y + 0.3, z, 0xFFFFFF);
                        Queue<IMolangParticleInstance> queue = MolangParticleEngine.INSTANCE.getParticlesForEmitter(emitter);
                        int count = queue == null ? 0 : queue.size();
                        DebugRenderer.renderFloatingText(poseStack, bufferSource, "particles: " + count, x, y + 0.1, z, count >= emitter.particleGroup.getLimit() ? 0xFF0000 : 0xFFFFFF);
                        poseStack.pushPose();
                        poseStack.translate(x - camX, y - camY, z - camZ);
                        LevelRenderer.renderLineBox(poseStack, bufferSource.getBuffer(RenderType.lines()), -0.5, -0.5, -0.5, 0.5, 0.5, 0.5, 0, 1, 0, 1);
                        poseStack.popPose();
                    }
                }
            }
        }
    }
}

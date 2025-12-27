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
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.mesdag.particlestorm.api.IComponent;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.data.component.*;
import org.mesdag.particlestorm.data.event.*;
import org.mesdag.particlestorm.particle.MolangParticleLoader;
import org.mesdag.particlestorm.particle.ParticleEmitter;

@Mod.EventBusSubscriber(modid = ParticleStorm.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PSGameClient {
    public static final MolangParticleLoader LOADER = new MolangParticleLoader();
    public static final ParticleRenderType PARTICLE_ADD = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder builder, TextureManager textureManager) {
            RenderSystem.enableDepthTest();
            Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
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

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            PSClientConfigs.onLoad();
            MinecraftForge.EVENT_BUS.addListener(PSGameClient::tick);
            MinecraftForge.EVENT_BUS.addListener(PSGameClient::renderLevelStage);
        });
    }

    @SubscribeEvent
    public static void modConfig$Reloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getModId().equals(ParticleStorm.MODID)) {
            PSClientConfigs.onLoad();
        }
    }

    private static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer == null) {
            LOADER.removeAll();
        } else if (!minecraft.isPaused() && runsNormally()) {
            LOADER.tick(localPlayer);
        }
    }

    private static void renderLevelStage(RenderLevelStageEvent event) {
        if (!PSClientConfigs.showEmitterOutline) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES && minecraft.getEntityRenderDispatcher().shouldRenderHitBoxes()) {
            float partialTicks = event.getPartialTick();
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

    private static void registerComponents() {
        IComponent.register("emitter_local_space", EmitterLocalSpace::fromJson);
        IComponent.register("emitter_initialization", EmitterInitialization::fromJson);

        IComponent.register("emitter_rate_instant", EmitterRate.Instant::fromJson);
        IComponent.register("emitter_rate_steady", EmitterRate.Steady::fromJson);
        IComponent.register("emitter_rate_manual", EmitterRate.Manual::fromJson);

        IComponent.register("emitter_lifetime_looping", EmitterLifetime.Looping::fromJson);
        IComponent.register("emitter_lifetime_once", EmitterLifetime.Once::fromJson);
        IComponent.register("emitter_lifetime_expression", EmitterLifetime.Expression::fromJson);
        IComponent.register("emitter_lifetime_events", EmitterLifetimeEvents::fromJson);

        IComponent.register("emitter_shape_point", EmitterShape.Point::fromJson);
        IComponent.register("emitter_shape_sphere", EmitterShape.Sphere::fromJson);
        IComponent.register("emitter_shape_box", EmitterShape.Box::fromJson);
        IComponent.register("emitter_shape_entity_aabb", EmitterShape.EntityAABB::fromJson);
        IComponent.register("emitter_shape_disc", EmitterShape.Disc::fromJson);

        IComponent.register("particle_initial_speed", ParticleInitialSpeed::fromJson);
        IComponent.register("particle_initial_spin", ParticleInitialSpin::fromJson);
        IComponent.register("particle_initialization", ParticleInitialization::fromJson);

        IComponent.register(ParticleMotionDynamic.ID, ParticleMotionDynamic::fromJson);
        IComponent.register("particle_motion_parametric", ParticleMotionParametric::fromJson);
        IComponent.register(ParticleMotionCollision.ID, ParticleMotionCollision::fromJson);

        IComponent.register(ParticleAppearanceBillboard.ID, ParticleAppearanceBillboard::fromJson);
        IComponent.register("particle_appearance_tinting", ParticleAppearanceTinting::fromJson);
        IComponent.register("particle_appearance_lighting", ParticleAppearanceLighting::fromJson);

        IComponent.register("particle_lifetime_expression", ParticleLifetimeExpression::fromJson);
        IComponent.register(ParticleLifeTimeEvents.ID, ParticleLifeTimeEvents::fromJson);
        IComponent.register("particle_kill_plane", ParticleLifetimeKillPlane::fromJson);
        IComponent.register("particle_expire_if_in_blocks", ParticleExpireIfInBlocks::fromJson);
        IComponent.register("particle_expire_if_not_in_blocks", ParticleExpireIfNotInBlocks::fromJson);
    }

    private static void registerEventNodes() {
        IEventNode.register("sequence", EventSequence::fromJson);
        IEventNode.register("weight", EventRandomize.Weight::fromJson);
        IEventNode.register("randomize", EventRandomize::fromJson);
        IEventNode.register("particle_effect", ParticleEffect::fromJson);
        IEventNode.register("sound_effect", SoundEffect::fromJson);
        IEventNode.register("expression", NodeMolangExp::fromJson);
        IEventNode.register("log", EventLog::fromJson);
    }

    public static float tickRate() {
        return 20;
    }

    public static boolean runsNormally() {
        return true;
    }
}

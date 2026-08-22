package org.mesdag.particlestorm.api.geckolib;

import com.mojang.datafixers.DSL;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4x3f;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.mixed.*;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.mesdag.particlestorm.particle.ParticleEmitter;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.keyframe.event.data.ParticleKeyframeData;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.util.List;
import java.util.Map;

public final class GeckoLibHelper {
    static RegistryObject<BlockEntityType<TestBlock.Entity>> TEST_ENTITY;

    public static void registerStuffs(IEventBus bus) {
        DeferredRegister<Block> BLOCK = DeferredRegister.create(Registries.BLOCK, ParticleStorm.MODID);
        DeferredRegister<BlockEntityType<?>> ENTITY = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ParticleStorm.MODID);
        RegistryObject<Block> TEST = BLOCK.register("test_block", TestBlock::new);
        TEST_ENTITY = ENTITY.register("test_entity", () -> BlockEntityType.Builder.of(TestBlock.Entity::new, TEST.get()).build(DSL.remainderType()));
        BLOCK.register(bus);
        ENTITY.register(bus);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TEST_ENTITY.get(), ExampleBlockEntityRenderer::new);
        event.registerEntityRenderer(EntityType.CREEPER, ReplacedCreeperRenderer::new);
    }

    public static void postEvent() {
        ModLoader.get().postEvent(new RegisterLocatorPreTransformerEvent());
    }

    public static double[] getLocatorOffset(LocatorValue locatorValue) {
        if (locatorValue.locatorClass() == null) {
            return locatorValue.values();
        }
        return locatorValue.locatorClass().offset();
    }

    public static double[] getLocatorRotation(LocatorValue locatorValue) {
        if (locatorValue.locatorClass() == null) {
            return new double[3];
        }
        return locatorValue.locatorClass().rotation();
    }

    /// @return true means failed to add emitter
    public static boolean processParticleEffect(@Nullable GeoAnimatable animatable, AnimationController<?> controller, ParticleKeyframeData keyframeData) {
        List<GeoBone> bones = IPSAnimationController.of(controller).particlestorm$getBonesWhichHasLocators();
        if (bones.isEmpty()) return true;

        IPSParticleKeyframeData iData = IPSParticleKeyframeData.of(keyframeData);
        Entity entity;
        BlockEntity blockEntity;
        VariableTable variableTable;
        Level level;
        if (animatable instanceof Entity entity1) {
            entity = entity1;
            blockEntity = null;
            variableTable = IPSEntity.of(entity).particlestorm$getVariableTable();
            level = entity.level();
        } else if (animatable instanceof ParticleStormGeoReplacedEntity withCurrentEntity && withCurrentEntity.getCurrentEntity() != null) {
            entity = withCurrentEntity.getCurrentEntity();
            blockEntity = null;
            variableTable = IPSEntity.of(entity).particlestorm$getVariableTable();
            level = entity.level();
        } else if (animatable instanceof BlockEntity entity1 && entity1.getLevel() != null) {
            entity = null;
            blockEntity = entity1;
            variableTable = IPSBlockEntity.of(blockEntity).particlestorm$getVariableTable();
            level = blockEntity.getLevel();
        } else {
            return true;
        }
        ResourceLocation particle = iData.particlestorm$getParticle();
        MolangExp expression = iData.particlestorm$getExpression(variableTable);
        IPSAnimatableInstanceCache cache = IPSAnimatableInstanceCache.of(animatable.getAnimatableInstanceCache());
        for (GeoBone bone : bones) {
            LocatorValue locator = IPSGeoBone.of(bone).particlestorm$getLocators().get(keyframeData.getLocator());
            if (locator == null) continue;

            ParticleEmitter current = MolangParticleEngine.INSTANCE.getEmitter(cache.particlestorm$getCachedId().getInt(locator));
            if (current == null || current.isRemoved() || !particle.equals(current.particleId)) {
                Vec3 pos = entity == null ?
                        new Vec3(blockEntity.getBlockPos().getX() + 0.5, 0, blockEntity.getBlockPos().getZ() + 0.5)
                        : entity.position();
                ParticleEmitter emitter = new ParticleEmitter(level, pos, particle, expression);
                MolangParticleEngine.INSTANCE.addEmitter(emitter);
                cache.particlestorm$getCachedId().put(locator, emitter.id);
                emitter.attachEntity(entity);
                emitter.attachBlock(blockEntity);
                double[] offset = getLocatorOffset(locator);
                double[] rotation = getLocatorRotation(locator);
                LocatorState state = cache.particlestorm$getLocatorState(locator);
                state.px = -(float) (offset[0] * 0.0625);
                state.py = (float) (offset[1] * 0.0625);
                state.pz = (float) (offset[2] * 0.0625);
                state.rx = (float) Math.toRadians(rotation[0]);
                state.ry = (float) Math.toRadians(rotation[1]);
                state.rz = (float) Math.toRadians(rotation[2]);
            }
        }
        return false;
    }

    public static void setCurrentEntity(GeoAnimatable animatable, @Nullable Entity entity) {
        if (animatable instanceof ParticleStormGeoReplacedEntity withCurrentEntity) {
            withCurrentEntity.setCurrentEntity(entity);
        }
    }

    public static void removeEmittersWhenAnimationChange(AnimationController.State animationState, AnimatableInstanceCache animatableInstanceCache) {
        if (animationState == AnimationController.State.TRANSITIONING) {
            IntIterator iterator = IPSAnimatableInstanceCache.of(animatableInstanceCache).particlestorm$getCachedId().values().iterator();
            while (iterator.hasNext()) {
                MolangParticleEngine.INSTANCE.removeEmitter(iterator.nextInt(), false);
                iterator.remove();
            }
        }
    }

    private static final Matrix4x3f mat = new Matrix4x3f();

    public static void transformLocator(GeoBone bone, GeoAnimatable animatable, float partialTick) {
        Map<String, LocatorValue> locators = IPSGeoBone.of(bone).particlestorm$getLocators();
        if (locators == null || locators.isEmpty()) return;
        mat.identity();
        RegisterLocatorPreTransformerEvent.getTransformer(animatable).transform(bone, animatable, mat, partialTick);
        IPSAnimatableInstanceCache cache = IPSAnimatableInstanceCache.of(animatable.getAnimatableInstanceCache());
        for (LocatorValue locator : locators.values()) {
            ParticleEmitter emitter = MolangParticleEngine.INSTANCE.getEmitter(cache.particlestorm$getCachedId().getInt(locator));
            if (emitter == null || emitter.isRemoved()) continue;
            LocatorState state = cache.particlestorm$getLocatorState(locator);
            emitter.setLocalSpace(new Matrix4x3f(mat)
                    .rotateXYZ(state.rx, state.ry, state.rz)
                    .translate(state.px, state.py, state.pz), false);
        }
    }

    public static class LocatorState {
        float px, py, pz;
        float rx, ry, rz;
    }
}

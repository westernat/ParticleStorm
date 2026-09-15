package org.mesdag.particlestorm.api.geckolib;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4x3f;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.mixed.*;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.mesdag.particlestorm.particle.ParticleEmitter;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.keyframe.event.data.ParticleKeyframeData;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GeckoLibHelper {
    static DeferredHolder<BlockEntityType<?>, BlockEntityType<TestBlock.Entity>> TEST_ENTITY;

    public static void registerStuffs(IEventBus bus) {
        DeferredRegister<Block> BLOCK = DeferredRegister.create(Registries.BLOCK, ParticleStorm.MODID);
        DeferredRegister<BlockEntityType<?>> ENTITY = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ParticleStorm.MODID);
        DeferredHolder<Block, Block> TEST = BLOCK.register("test_block", TestBlock::new);
        TEST_ENTITY = ENTITY.register("test_entity", () -> BlockEntityType.Builder.of(TestBlock.Entity::new, TEST.get()).build(DSL.remainderType()));
        BLOCK.register(bus);
        ENTITY.register(bus);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TEST_ENTITY.get(), ExampleBlockEntityRenderer::new);
        event.registerEntityRenderer(EntityType.CREEPER, ReplacedCreeperRenderer::new);
    }

    public static void postEvent() {
        ModLoader.postEvent(new RegisterLocatorPreTransformerEvent());
    }

    /// @return true means failed to add emitter
    public static boolean processParticleEffect(@Nullable GeoAnimatable animatable, AnimationController<?> controller, ParticleKeyframeData keyframeData) {
        List<GeoBone> bones = IPSAnimationController.of(controller).particlestorm$getBonesWhichHasLocators();
        if (bones.isEmpty()) return true;

        IPSParticleKeyframeData iData = IPSParticleKeyframeData.of(keyframeData);
        Either<Entity, BlockEntity> either;
        VariableTable variableTable;
        Level level;
        switch (animatable) {
            case Entity entity -> {
                either = Either.left(entity);
                variableTable = IPSEntity.of(entity).particlestorm$getVariableTable();
                level = entity.level();
            }
            case WithCurrentEntity withCurrentEntity -> {
                Entity entity = withCurrentEntity.getCurrentEntity();
                if (entity == null) return true;
                either = Either.left(entity);
                variableTable = IPSEntity.of(entity).particlestorm$getVariableTable();
                level = entity.level();
            }
            case BlockEntity entity when entity.getLevel() != null -> {
                either = Either.right(entity);
                variableTable = IPSBlockEntity.of(entity).particlestorm$getVariableTable();
                level = entity.getLevel();
            }
            case null, default -> {
                return true;
            }
        }
        ResourceLocation particle = iData.particlestorm$getParticle();
        MolangExp expression = iData.particlestorm$getExpression(variableTable);
        IPSAnimatableInstanceCache cache = IPSAnimatableInstanceCache.of(animatable.getAnimatableInstanceCache());
        for (GeoBone bone : bones) {
            LocatorValue locator = IPSGeoBone.of(bone).particlestorm$getLocators().get(keyframeData.getLocator());
            if (locator == null) continue;
            Object2ObjectMap<LocatorValue, IntList> ids = cache.particlestorm$getCachedId();
            IntList integers = ids.computeIfAbsent(locator, l -> new IntArrayList());
            if (integers.isEmpty()) {
                createNeoOne(either, level, particle, expression, integers, locator, cache);
            } else {
                IntIterator ii = integers.intIterator();
                while (ii.hasNext()) {
                    ParticleEmitter current = MolangParticleEngine.INSTANCE.getEmitter(ii.nextInt());
                    if (current == null || current.isRemoved() || !particle.equals(current.particleId)) {
                        createNeoOne(either, level, particle, expression, integers, locator, cache);
                    }
                }
            }
        }
        return false;
    }

    private static void createNeoOne(Either<Entity, BlockEntity> either, Level level, ResourceLocation particle, MolangExp expression, IntList integers, LocatorValue locator, IPSAnimatableInstanceCache cache) {
        Vec3 pos = either.map(Entity::position, entity -> entity.getBlockPos().getBottomCenter());
        ParticleEmitter emitter = new ParticleEmitter(level, pos, particle, expression);
        MolangParticleEngine.INSTANCE.addEmitter(emitter);
        integers.add(emitter.id);
        either.ifLeft(emitter::attachEntity).ifRight(emitter::attachBlock);
        if (cache.particlestorm$getLocatorState(locator) == null) {
            cache.particlestorm$createLocatorState(locator);
        }
    }

    public static void setCurrentEntity(GeoAnimatable animatable, @Nullable Entity entity) {
        if (animatable instanceof WithCurrentEntity withCurrentEntity) {
            withCurrentEntity.setCurrentEntity(entity);
        }
    }

    public static void removeEmittersWhenAnimationChange(AnimationController.State state, AnimatableInstanceCache cache) {
        if (state == AnimationController.State.TRANSITIONING) {
            Object2ObjectMap<LocatorValue, IntList> ids = IPSAnimatableInstanceCache.of(cache).particlestorm$getCachedId();
            if (ids.isEmpty()) return;
            for (IntList integers : ids.values()) {
                IntIterator ii = integers.intIterator();
                while (ii.hasNext()) {
                    MolangParticleEngine.INSTANCE.removeEmitter(ii.nextInt(), false);
                }
            }
            ids.clear();
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
            IntList integers = cache.particlestorm$getCachedId().get(locator);
            if (integers == null || integers.isEmpty()) continue;
            IntIterator iter = integers.intIterator();
            while (iter.hasNext()) {
                ParticleEmitter emitter = MolangParticleEngine.INSTANCE.getEmitter(iter.nextInt());
                if (emitter == null || emitter.isRemoved()) continue;
                LocatorState state = cache.particlestorm$getLocatorState(locator);
                if (state == null) continue;
                emitter.setLocalSpace(new Matrix4x3f(mat)
                        .rotateXYZ(state.rx, state.ry, state.rz)
                        .translate(state.px, state.py, state.pz), true);
            }
        }
    }

    private static final List<Runnable> runners = new ArrayList<>();

    public static void addReloadCallback(Runnable runner) {
        runners.add(runner);
    }

    public static void clearReloadCallbacks() {
        runners.clear();
    }

    public static void afterReload() {
        for (Runnable runner : runners) {
            runner.run();
        }
    }

    public static class LocatorState {
        private final float px, py, pz;
        private final float rx, ry, rz;

        public LocatorState(LocatorValue locator) {
            double[] offset = getLocatorOffset(locator);
            double[] rotation = getLocatorRotation(locator);
            this.px = -(float) (offset[0] * 0.0625);
            this.py = (float) (offset[1] * 0.0625);
            this.pz = (float) (offset[2] * 0.0625);
            this.rx = (float) Math.toRadians(rotation[0]);
            this.ry = (float) Math.toRadians(rotation[1]);
            this.rz = (float) Math.toRadians(rotation[2]);
        }

        private static double[] getLocatorOffset(LocatorValue locatorValue) {
            if (locatorValue.locatorClass() == null) {
                return locatorValue.values();
            }
            return locatorValue.locatorClass().offset();
        }

        private static double[] getLocatorRotation(LocatorValue locatorValue) {
            if (locatorValue.locatorClass() == null) {
                return new double[3];
            }
            return locatorValue.locatorClass().rotation();
        }
    }
}

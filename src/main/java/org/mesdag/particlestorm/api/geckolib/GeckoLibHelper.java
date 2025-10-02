package org.mesdag.particlestorm.api.geckolib;

import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.mixed.*;
import org.mesdag.particlestorm.particle.ParticleEmitter;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.keyframe.event.ParticleKeyframeEvent;
import software.bernie.geckolib.animation.keyframe.event.data.ParticleKeyframeData;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.util.List;

public final class GeckoLibHelper {
    public static double[] getLocatorOffset(Object locatorValue) {
        LocatorValue value = (LocatorValue) locatorValue;
        if (value.locatorClass() == null) {
            return value.values();
        }
        return value.locatorClass().offset();
    }

    public static double[] getLocatorRotation(Object locatorValue) {
        LocatorValue value = (LocatorValue) locatorValue;
        if (value.locatorClass() == null) {
            return new double[3];
        }
        return value.locatorClass().rotation();
    }

    /**
     * todo software.bernie.geckolib.loading.json.raw.LocatorValue#locatorClass
     *
     * @return true means failed to add emitter
     */
    public static boolean processParticleEffect(Object particleKeyframeEvent) {
        ParticleKeyframeEvent<?> event = (ParticleKeyframeEvent<?>) particleKeyframeEvent;
        List<GeoBone> bones = ((IAnimationController) event.getController()).particlestorm$getBonesWhichHasLocators();
        if (bones.isEmpty()) return true;

        ParticleKeyframeData keyframeData = event.getKeyframeData();
        IParticleKeyframeData iData = (IParticleKeyframeData) keyframeData;
        Entity entity = null;
        BlockEntity blockEntity = null;
        VariableTable variableTable;
        Level level;
        GeoAnimatable animatable = event.getAnimatable();
        switch (animatable) {
            case Entity entity1 -> {
                entity = entity1;
                variableTable = IEntity.of(entity).particlestorm$getVariableTable();
                level = entity.level();
            }
            case GeoWithCurrentEntity withCurrentEntity when withCurrentEntity.getCurrentEntity() != null -> {
                entity = withCurrentEntity.getCurrentEntity();
                variableTable = IEntity.of(entity).particlestorm$getVariableTable();
                level = entity.level();
            }
            case BlockEntity entity1 when entity1.getLevel() != null -> {
                blockEntity = entity1;
                variableTable = ((IBlockEntity) blockEntity).particlestorm$getVariableTable();
                level = blockEntity.getLevel();
            }
            case null, default -> {
                return true;
            }
        }
        ResourceLocation particle = iData.particlestorm$getParticle();
        MolangExp expression = iData.particlestorm$getExpression(variableTable);
        IAnimatableInstanceCache cache = IAnimatableInstanceCache.of(animatable.getAnimatableInstanceCache());
        for (GeoBone geoBone : bones) {
            IGeoBone bone = IGeoBone.of(geoBone);
            LocatorValue locator = bone.particlestorm$getLocators().get(keyframeData.getLocator());
            if (locator == null) continue;

            ParticleEmitter current = PSGameClient.LOADER.getEmitter(cache.particlestorm$getCachedId().getInt(locator));
            if (current == null || current.isRemoved() || !particle.equals(current.particleId)) {
                Vec3 pos = entity == null ? blockEntity.getBlockPos().getBottomCenter() : entity.position();
                ParticleEmitter emitter = new ParticleEmitter(level, pos, particle, expression);
                PSGameClient.LOADER.addEmitter(emitter, false);
                cache.particlestorm$getCachedId().put(locator, emitter.id);
                emitter.attachEntity(entity);
                emitter.attachedBlock = blockEntity;
                double[] offset = getLocatorOffset(locator);
                double[] rotation = getLocatorRotation(locator);
                emitter.offsetPos = new Vec3(offset[0] * 0.0625, offset[1] * 0.0625, -offset[2] * 0.0625);
                emitter.offsetRot = new Vector3f((float) Math.toRadians(rotation[0]), (float) Math.toRadians(rotation[1]), (float) Math.toRadians(rotation[2]));
                emitter.parentPosition = cache.particlestorm$getPosition();
                emitter.parentRotation = cache.particlestorm$getRotation();
                emitter.parentMode = ParticleEmitter.ParentMode.LOCATOR;
            }
        }
        return false;
    }

    public static void setCurrentEntity(Object animatable, Entity entity) {
        if (animatable instanceof GeoWithCurrentEntity withCurrentEntity) {
            withCurrentEntity.setCurrentEntity(entity);
        }
    }

    public static void removeEmittersWhenAnimationChange(int size, Object animationState, Object animatableInstanceCache) {
        if (size > 0 && animationState == AnimationController.State.TRANSITIONING) {
            IntIterator iterator = IAnimatableInstanceCache.of((AnimatableInstanceCache) animatableInstanceCache).particlestorm$getCachedId().values().iterator();
            while (iterator.hasNext()) {
                PSGameClient.LOADER.removeEmitter(iterator.nextInt(), false);
                iterator.remove();
            }
        }
    }
}

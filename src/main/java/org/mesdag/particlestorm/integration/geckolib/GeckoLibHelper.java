package org.mesdag.particlestorm.integration.geckolib;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.joml.Vector3f;
import org.mesdag.particlestorm.GameClient;
import org.mesdag.particlestorm.data.event.ParticleEffect;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.mixinauxi.IAnimationController;
import org.mesdag.particlestorm.mixinauxi.IEntity;
import org.mesdag.particlestorm.mixinauxi.IGeoBone;
import org.mesdag.particlestorm.mixinauxi.IParticleKeyframeData;
import org.mesdag.particlestorm.particle.ParticleEmitter;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.keyframe.event.ParticleKeyframeEvent;
import software.bernie.geckolib.animation.keyframe.event.data.ParticleKeyframeData;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.loading.json.raw.LocatorValue;

import java.util.List;
import java.util.Set;

public final class GeckoLibHelper {
    private static final double[] ZERO = new double[3];
    private static Boolean isLoaded;

    public static boolean isLoaded() {
        if (isLoaded == null) {
            isLoaded = ModList.get().isLoaded("geckolib");
        }
        return isLoaded;
    }

    public static double[] getLocatorOffset(Object locatorValue) {
        if (isLoaded() && locatorValue instanceof LocatorValue value) {
            if (value.locatorClass() == null) {
                return value.values();
            }
            return value.locatorClass().offset();
        }
        return ZERO;
    }

    public static double[] getLocatorRotation(Object locatorValue) {
        if (isLoaded() && locatorValue instanceof LocatorValue value) {
            if (value.locatorClass() == null) {
                return ZERO;
            }
            return value.locatorClass().rotation();
        }
        return ZERO;
    }

    /**
     * @return true means failed to add emitter
     */
    public static boolean processParticleEffect(Object particleKeyframeEvent) {
        if (isLoaded() && particleKeyframeEvent instanceof ParticleKeyframeEvent<?> event) {
            ParticleKeyframeData keyframeData = event.getKeyframeData();
            IParticleKeyframeData iData = (IParticleKeyframeData) keyframeData;
            Entity entity;
            if (event.getAnimatable() instanceof Entity entity1) {
                entity = entity1;
            } else if (event.getAnimatable() instanceof GeoWithCurrentEntity withCurrentEntity && withCurrentEntity.getCurrentEntity() != null) {
                entity = withCurrentEntity.getCurrentEntity();
            } else {
                return true;
            }
            List<GeoBone> bones = ((IAnimationController) event.getController()).particlestorm$getBonesWhichHasLocators();
            if (bones.isEmpty()) return true;
            ResourceLocation particle = iData.particlestorm$getParticle();
            MolangExp expression = iData.particlestorm$getExpression(entity);
            int[] cachedId = iData.particlestorm$getCachedId(bones.size());
            for (int i = 0; i < cachedId.length; i++) {
                int id = cachedId[i];
                if (GameClient.LOADER.contains(id)) continue;
                GeoBone bone = bones.get(i);
                LocatorValue locator = ((IGeoBone) bone).particlestorm$getLocators().get(keyframeData.getLocator());
                if (locator == null) continue;
                ParticleEmitter emitter = new ParticleEmitter(entity.level(), Vec3.ZERO, particle, ParticleEffect.Type.EMITTER, expression);
                emitter.subTable = ((IEntity) entity).particlestorm$getVariableTable();
                GameClient.LOADER.addEmitter(emitter, false);
                cachedId[i] = emitter.id;
                double[] offset = getLocatorOffset(locator);
                double[] rotation = getLocatorRotation(locator);
                emitter.offsetPos = new Vec3(offset[0] * 0.0625, offset[1] * 0.0625, -offset[2] * 0.0625);
                emitter.offsetRot = new Vector3f((float) Math.toRadians(rotation[0]), (float) Math.toRadians(rotation[1]), (float) Math.toRadians(rotation[2]));
                emitter.modelSpace = bone.getModelSpaceMatrix();
                emitter.attached = entity;
                emitter.parentMode = ParticleEmitter.ParentMode.LOCATOR;
            }
            return false;
        }
        return true;
    }

    public static void setCurrentEntity(Object animatable, Entity entity) {
        if (isLoaded() && animatable instanceof GeoWithCurrentEntity withCurrentEntity) {
            withCurrentEntity.setCurrentEntity(entity);
        }
    }

    public static void removeEmittersWhenAnimationChange(int size, Object animationState, Set<?> executedKeyFrames) {
        if (isLoaded() && size > 0 && animationState == AnimationController.State.TRANSITIONING) {
            for (Object executedKeyFrame : executedKeyFrames) {
                if (executedKeyFrame instanceof ParticleKeyframeData particleKeyframeData) {
                    int[] cachedId = ((IParticleKeyframeData) particleKeyframeData).particlestorm$getCachedId(size);
                    for (int id : cachedId) {
                        if (id != -1) {
                            GameClient.LOADER.removeEmitter(id, false);
                        }
                    }
                }
            }
        }
    }
}

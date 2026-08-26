package org.mesdag.particlestorm.api.geckolib;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.joml.Matrix4x3f;
import org.joml.Quaternionf;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtil;

import java.util.ArrayDeque;
import java.util.Deque;

public class RegisterLocatorPreTransformerEvent extends Event implements IModBusEvent {
    private static final Reference2ObjectMap<AnimatableInstanceCache, Transformer<?>> singletonTransformers = new Reference2ObjectOpenHashMap<>();
    private static final Reference2ObjectMap<BlockEntityType<?>, Transformer<?>> blockEntityTransformers = new Reference2ObjectOpenHashMap<>();
    private static final Reference2ObjectMap<EntityType<?>, Transformer<?>> entityTransformers = new Reference2ObjectOpenHashMap<>();

    RegisterLocatorPreTransformerEvent() {}

    public <T extends Item & GeoItem> void register(T item, Transformer<T> transformer) {
        singletonTransformers.put(item.getAnimatableInstanceCache(), transformer);
    }

    public <T extends BlockEntity & GeoBlockEntity> void register(BlockEntityType<? extends T> blockEntity, Transformer<T> transformer) {
        blockEntityTransformers.put(blockEntity, transformer);
    }

    public <T extends Entity & GeoEntity> void register(EntityType<? extends T> entity, Transformer<T> transformer) {
        entityTransformers.put(entity, transformer);
    }

    public <T extends GeoAnimatable & WithCurrentEntity> void register(T replacedEntity, Transformer<T> transformer) {
        singletonTransformers.put(replacedEntity.getAnimatableInstanceCache(), transformer);
    }

    @SuppressWarnings("unchecked")
    public static <T extends GeoAnimatable> Transformer<T> getTransformer(T animatable) {
        Transformer<?> transformer = switch (animatable) {
            case Entity entity -> entityTransformers.getOrDefault(entity.getType(), Transformer::entityTransformer);
            case BlockEntity blockEntity -> blockEntityTransformers.getOrDefault(blockEntity.getType(), Transformer::defaultTransformer);
            case Item ignored -> singletonTransformers.getOrDefault(animatable.getAnimatableInstanceCache(), Transformer::defaultTransformer);
            case WithCurrentEntity ignored -> singletonTransformers.getOrDefault(animatable.getAnimatableInstanceCache(), Transformer::replacedEntityTransformer);
            default -> Transformer::defaultTransformer;
        };
        return (Transformer<T>) transformer;
    }

    private static final Quaternionf quat = new Quaternionf();

    @FunctionalInterface
    public interface Transformer<T extends GeoAnimatable> {
        void transform(GeoBone bone, T animatable, Matrix4x3f mat, float partialTick);

        static void replacedEntityTransformer(GeoBone bone, GeoAnimatable animatable, Matrix4x3f mat, float partialTick) {
            Entity entity = ((WithCurrentEntity) animatable).getCurrentEntity();
            if (entity != null) {
                transformEntity(entity, mat, partialTick);
            }
            defaultTransformer(bone, animatable, mat, partialTick);
        }

        static void entityTransformer(GeoBone bone, GeoAnimatable animatable, Matrix4x3f mat, float partialTick) {
            transformEntity((Entity) animatable, mat, partialTick);
            defaultTransformer(bone, animatable, mat, partialTick);
        }

        /// [software.bernie.geckolib.renderer.GeoEntityRenderer#actuallyRender]
        static void transformEntity(Entity entity, Matrix4x3f mat, float partialTick) {
            LivingEntity living = entity instanceof LivingEntity livingEntity ? livingEntity : null;
            boolean shouldSit = entity.isPassenger() && (entity.getVehicle() != null);
            float lerpBodyRot = living == null ? 0 : Mth.lerp(partialTick, living.yBodyRotO, living.yBodyRot);
            float lerpHeadRot = living == null ? 0 : Mth.lerp(partialTick, living.yHeadRotO, living.yHeadRot);

            if (shouldSit && entity.getVehicle() instanceof LivingEntity livingEntity) {
                lerpBodyRot = Mth.rotLerp(partialTick, livingEntity.yBodyRotO, livingEntity.yBodyRot);
                float netHeadYaw = lerpHeadRot - lerpBodyRot;
                float clampedHeadYaw = Mth.clamp(Mth.wrapDegrees(netHeadYaw), -85, 85);
                lerpBodyRot = lerpHeadRot - clampedHeadYaw;

                if (clampedHeadYaw * clampedHeadYaw > 2500f) {
                    lerpBodyRot += clampedHeadYaw * 0.2f;
                }
            }
            if (entity.getPose() == Pose.SLEEPING && living != null) {
                Direction bedDirection = living.getBedOrientation();
                if (bedDirection != null) {
                    float eyePosOffset = living.getEyeHeight(Pose.STANDING) - 0.1F;
                    mat.translate(-bedDirection.getStepX() * eyePosOffset, 0, -bedDirection.getStepZ() * eyePosOffset);
                }
            }
            float nativeScale = living != null ? living.getScale() : 1;
            mat.scale(nativeScale, nativeScale, nativeScale);
            if (entity.isFullyFrozen()) {
                lerpBodyRot += Mth.cos(entity.tickCount * 3.25F) * Mth.PI * 0.4F;
            }
            if (!entity.hasPose(Pose.SLEEPING)) {
                mat.rotate(quat.rotationY((180 - lerpBodyRot) * Mth.DEG_TO_RAD));
            }
            if (living != null) {
                if (living.deathTime > 0) {
                    float deathRotation = (living.deathTime + partialTick - 1) / 20 * 1.6f;
                    mat.rotate(quat.rotationZ(Math.min(Mth.sqrt(deathRotation), 1) * Mth.HALF_PI));
                } else if (living.isAutoSpinAttack()) {
                    mat.rotate(quat.rotationX(living.getXRot() * -Mth.DEG_TO_RAD - Mth.HALF_PI));
                    mat.rotate(quat.rotationY((living.tickCount + partialTick) * 75 * -Mth.DEG_TO_RAD));
                } else if (entity.hasPose(Pose.SLEEPING)) {
                    Direction bedOrientation = living.getBedOrientation();

                    mat.rotate(quat.rotationY((bedOrientation != null ? RenderUtil.getDirectionAngle(bedOrientation) : lerpBodyRot) * Mth.DEG_TO_RAD));
                    mat.rotate(quat.rotationZ(Mth.HALF_PI));
                    mat.rotate(quat.rotationY(Mth.PI * 1.5F));
                } else if (LivingEntityRenderer.isEntityUpsideDown(living)) {
                    mat.translate(0, (entity.getBbHeight() + 0.1f) / nativeScale, 0);
                    mat.rotate(quat.rotationZ(Mth.PI));
                }
            }
            mat.translate(0, 0.01f, 0);
        }

        static void defaultTransformer(GeoBone bone, GeoAnimatable animatable, Matrix4x3f mat, float partialTick) {
            Deque<GeoBone> chain = new ArrayDeque<>();
            GeoBone current = bone;
            while (current != null) {
                chain.add(current);
                current = current.getParent();
            }
            while (!chain.isEmpty()) {
                GeoBone last = chain.pollLast();
                mat.translate(-last.getPosX() / 16f, last.getPosY() / 16f, last.getPosZ() / 16f);
                mat.translate(last.getPivotX() / 16f, last.getPivotY() / 16f, last.getPivotZ() / 16f);
                if (last.getRotZ() != 0) {
                    mat.rotate(quat.rotationZ(last.getRotZ()));
                }
                if (last.getRotY() != 0) {
                    mat.rotate(quat.rotationY(last.getRotY()));
                }
                if (last.getRotX() != 0) {
                    mat.rotate(quat.rotationX(last.getRotX()));
                }
                mat.scale(last.getScaleX(), last.getScaleY(), last.getScaleZ());
                mat.translate(-last.getPivotX() / 16f, -last.getPivotY() / 16f, -last.getPivotZ() / 16f);
            }
        }
    }
}

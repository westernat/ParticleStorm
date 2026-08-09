package org.mesdag.particlestorm.api.geckolib;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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

    public <T extends ParticleStormGeoReplacedEntity> void register(T replacedEntity, Transformer<T> transformer) {
        singletonTransformers.put(replacedEntity.getAnimatableInstanceCache(), transformer);
    }

    @SuppressWarnings("unchecked")
    public static <T extends GeoAnimatable> Transformer<T> getTransformer(T animatable) {
        Transformer<?> transformer = switch (animatable) {
            case Entity entity -> entityTransformers.getOrDefault(entity.getType(), Transformer::entityTransformer);
            case BlockEntity blockEntity -> blockEntityTransformers.getOrDefault(blockEntity.getType(), Transformer::defaultTransformer);
            case Item ignored -> singletonTransformers.getOrDefault(animatable.getAnimatableInstanceCache(), Transformer::defaultTransformer);
            case ParticleStormGeoReplacedEntity ignored -> singletonTransformers.getOrDefault(animatable.getAnimatableInstanceCache(), Transformer::replacedEntityTransformer);
            default -> Transformer::defaultTransformer;
        };
        return (Transformer<T>) transformer;
    }

    @FunctionalInterface
    public interface Transformer<T extends GeoAnimatable> {
        void transform(GeoBone bone, T animatable, PoseStack poseStack, float partialTick);

        static void replacedEntityTransformer(GeoBone bone, GeoAnimatable animatable, PoseStack poseStack, float partialTick) {
            Entity entity = ((ParticleStormGeoReplacedEntity) animatable).getCurrentEntity();
            if (entity != null) {
                transformEntity(entity, poseStack, partialTick);
            }
            defaultTransformer(bone, animatable, poseStack, partialTick);
        }

        static void entityTransformer(GeoBone bone, GeoAnimatable animatable, PoseStack poseStack, float partialTick) {
            transformEntity((Entity) animatable, poseStack, partialTick);
            defaultTransformer(bone, animatable, poseStack, partialTick);
        }

        /// [software.bernie.geckolib.renderer.GeoEntityRenderer#actuallyRender]
        static void transformEntity(Entity entity, PoseStack poseStack, float partialTick) {
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
                    poseStack.translate(-bedDirection.getStepX() * eyePosOffset, 0, -bedDirection.getStepZ() * eyePosOffset);
                }
            }
            float nativeScale = living != null ? living.getScale() : 1;
            poseStack.scale(nativeScale, nativeScale, nativeScale);
            if (entity.isFullyFrozen()) {
                lerpBodyRot += (float) (Math.cos(entity.tickCount * 3.25d) * Math.PI * 0.4d);
            }
            if (!entity.hasPose(Pose.SLEEPING)) {
                poseStack.mulPose(Axis.YP.rotationDegrees(180 - lerpBodyRot));
            }
            if (living != null) {
                if (living.deathTime > 0) {
                    float deathRotation = (living.deathTime + partialTick - 1f) / 20f * 1.6f;
                    poseStack.mulPose(Axis.ZP.rotationDegrees(Math.min(Mth.sqrt(deathRotation), 1) * 90));
                } else if (living.isAutoSpinAttack()) {
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90f - living.getXRot()));
                    poseStack.mulPose(Axis.YP.rotationDegrees((living.tickCount + partialTick) * -75f));
                } else if (entity.hasPose(Pose.SLEEPING)) {
                    Direction bedOrientation = living.getBedOrientation();

                    poseStack.mulPose(Axis.YP.rotationDegrees(bedOrientation != null ? RenderUtil.getDirectionAngle(bedOrientation) : lerpBodyRot));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                    poseStack.mulPose(Axis.YP.rotationDegrees(270f));
                } else if (LivingEntityRenderer.isEntityUpsideDown(living)) {
                    poseStack.translate(0, (entity.getBbHeight() + 0.1f) / nativeScale, 0);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(180f));
                }
            }
            poseStack.translate(0, 0.01f, 0);
        }

        static void defaultTransformer(GeoBone bone, GeoAnimatable animatable, PoseStack poseStack, float partialTick) {
            Deque<GeoBone> chain = new ArrayDeque<>();
            GeoBone current = bone;
            while (current != null) {
                chain.add(current);
                current = current.getParent();
            }
            while (!chain.isEmpty()) {
                RenderUtil.prepMatrixForBone(poseStack, chain.pollLast());
            }
        }
    }
}

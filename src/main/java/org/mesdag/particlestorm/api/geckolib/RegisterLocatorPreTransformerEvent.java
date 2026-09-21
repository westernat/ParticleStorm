package org.mesdag.particlestorm.api.geckolib;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
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
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import org.joml.Matrix4x3f;
import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.util.RenderUtil;

import java.util.ArrayDeque;
import java.util.Deque;

public class RegisterLocatorPreTransformerEvent {
    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class, listeners -> event -> {
        for (Callback listener : listeners) {
            listener.onRegisterLocatorPreTransformers(event);
        }
    });

    @FunctionalInterface
    public interface Callback {
        void onRegisterLocatorPreTransformers(RegisterLocatorPreTransformerEvent event);
    }

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

    @SuppressWarnings("unchecked")
    static <T extends GeoAnimatable> @Nullable Transformer<T> getRegisteredTransformer(T animatable) {
        Transformer<?> transformer = switch (animatable) {
            case Entity entity -> entityTransformers.get(entity.getType());
            case BlockEntity blockEntity -> blockEntityTransformers.get(blockEntity.getType());
            default -> singletonTransformers.get(animatable.getAnimatableInstanceCache());
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

        /// [com.geckolib.renderer.GeoEntityRenderer#actuallyRender]
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

                    float bedAngle = switch (bedOrientation) {
                        case SOUTH -> 90;
                        case NORTH -> 270;
                        case EAST -> 180;
                        case null -> lerpBodyRot;
                        default -> 0;
                    };
                    mat.rotate(quat.rotationY(bedAngle * Mth.DEG_TO_RAD));
                    mat.rotate(quat.rotationZ(Mth.HALF_PI));
                    mat.rotate(quat.rotationY(Mth.PI * 1.5F));
                } else if (isEntityUpsideDown(living)) {
                    mat.translate(0, (entity.getBbHeight() + 0.1f) / nativeScale, 0);
                    mat.rotate(quat.rotationZ(Mth.PI));
                }
            }
            mat.translate(0, 0.01f, 0);
        }

        @SuppressWarnings("unchecked")
        private static boolean isEntityUpsideDown(LivingEntity entity) {
            var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
            return renderer instanceof LivingEntityRenderer<?, ?, ?> livingRenderer
                    && ((LivingEntityRenderer<LivingEntity, ?, ?>) livingRenderer).isEntityUpsideDown(entity);
        }

        static void defaultTransformer(GeoBone bone, GeoAnimatable animatable, Matrix4x3f mat, float partialTick) {
            Deque<GeoBone> chain = new ArrayDeque<>();
            for (GeoBone current = bone; current != null; current = current.parent()) {
                chain.add(current);
            }
            PoseStack poseStack = new PoseStack();
            while (!chain.isEmpty()) {
                RenderUtil.prepMatrixForBone(poseStack, chain.pollLast());
            }
            mat.mul(poseStack.last().pose().get4x3(new Matrix4x3f()));
        }
    }
}

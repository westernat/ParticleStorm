package org.mesdag.particlestorm.api;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.mesdag.particlestorm.particle.ParticleEmitter;
import org.mesdag.particlestorm.particle.ParticlePreset;

import java.util.List;

public interface IMolangParticleInstance extends MolangInstance {
    default Particle self() {
        return (Particle) this;
    }

    int getAge();

    void setEmitter(ParticleEmitter emitter);

    ParticlePreset getPreset();

    @Nullable TextureAtlasSprite getSprite();

    Vector3f getAcceleration();

    Vector3f getFacingDirection();

    Vector3f getInitialSpeed();

    void setXRot(float x, boolean o);

    default void setXRot(float x) {
        setXRot(x, false);
    }

    void setYRot(float y, boolean o);

    default void setYRot(float y) {
        setYRot(y, false);
    }

    void setZRot(float z, boolean o);

    default void setZRot(float z) {
        setZRot(z, false);
    }

    void setZRotD(float delta);

    float getZRotD();

    void setCollisionDrag(float drag);

    void setCoefficientOfRestitution(float coefficient);

    void setExpireOnContact(boolean b);

    void setCollisionRadius(float radius);

    float getCollisionRadius();

    void setComponents(List<IParticleComponent> components);

    float getScaleU();

    float getScaleV();

    void setBillboardSize(float[] size);

    void setUvSize(float[] size);

    float[] getUvSize();

    void setUvStep(float[] step);

    float[] getUvStep();

    void setMaxFrame(int frame);

    int getMaxFrame();

    void setCurrentFrame(int frame);

    int getCurrentFrame();

    void setInsideKillPlane(boolean b);

    boolean isInsideKillPlane();

    void setParticleGroup(ParticleGroup group);

    void setLastTimeline(int last);

    int getLastTimeline();

    double getXd();

    double getYd();

    double getZd();

    double getX();

    double getY();

    double getZ();

    /// 粒子在世界空间中的坐标。
    ///
    /// 本地空间粒子（`emitter_local_space` 的 `position` 为 true）的 [MolangInstance#getX] 等是发射器本地坐标，
    /// 需要先用发射器的变换转换到世界坐标；朝向计算（例如看向摄像机的方向）必须基于世界坐标，
    /// 否则算出来的方向是错的。
    default Vector3f getWorldPosition(Vector3f dest, float partialTick) {
        return dest.set((float) getX(), (float) getY(), (float) getZ());
    }

    /// 粒子所在空间 -> 世界空间的旋转。
    ///
    /// 返回 null 表示粒子的朝向本来就定义在世界空间中（不是本地空间，或未启用 `local_rotation`）。
    /// [org.mesdag.particlestorm.particle.FaceCameraMode] 需要在这个空间里算朝向，
    /// 再由 [org.mesdag.particlestorm.particle.MolangParticleInstance#render] 统一绕粒子中心变换到世界空间。
    default @Nullable Quaternionf getLocalSpaceRotation() {
        return getEmitter().getLocalSpaceRotation();
    }

    void setPos(double x, double y, double z, boolean o);

    void setColor(float red, float green, float blue, float alpha);

    void setUV(float u, float v, float w, float h);

    void setCollision(boolean bool);

    void discard();

    boolean isDiscarded();

    // region default
    default void moveDirectly(double dx, double dy, double dz) {
        float radius = getCollisionRadius();
        double px = getX() + dx;
        double py = getY() + dy;
        double pz = getZ() + dz;
        self().setBoundingBox(new AABB(
                px - radius,
                py,
                pz - radius,
                px + radius,
                py + radius + radius,
                pz + radius
        ));
        self().setLocationFromBoundingbox();
    }

    @Override
    default float tickAge() {
        return getAge() * getInvTickRate();
    }

    @Override
    default float tickLifetime() {
        return self().getLifetime() * getInvTickRate();
    }

    @Override
    default ResourceLocation getIdentity() {
        return getEmitter().particleId;
    }

    @Override
    default Vec3 getPosition() {
        return self().getPos();
    }

    @Override
    default @Nullable ParticleEmitterAttachable getAttached() {
        return getEmitter().getAttached();
    }

    @Override
    default float getInvTickRate() {
        return getEmitter().invTickRate;
    }

    default boolean isVisible(Camera camera, Frustum frustum, float partialTick) {
        return frustum.isVisible(self().getBoundingBox().inflate(1));
    }
    // endregion
}

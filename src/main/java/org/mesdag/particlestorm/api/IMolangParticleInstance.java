package org.mesdag.particlestorm.api;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
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

    void setXRot(float x);

    void setYRot(float y);

    void setZRot(float z);

    void setZRotD(float delta);

    float getZRotD();

    void setCollisionDrag(float drag);

    void setCoefficientOfRestitution(float coefficient);

    void setExpireOnContact(boolean b);

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

    void setPosO(double x, double y, double z);

    void setColor(float red, float green, float blue, float alpha);

    void setUV(float u, float v, float w, float h);

    void setCollision(boolean bool);

    void discard();

    boolean isDiscarded();

    // region default
    default void moveDirectly(double x, double y, double z) {
        self().setBoundingBox(self().getBoundingBox().move(x, y, z));
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
    default @Nullable Entity getAttachedEntity() {
        return getEmitter().getAttachedEntity();
    }

    @Override
    default float getInvTickRate() {
        return getEmitter().invTickRate;
    }
    // endregion
}

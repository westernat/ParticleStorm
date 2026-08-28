package org.mesdag.particlestorm.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.component.ParticleMotionCollision;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.mixed.IPSTextureAtlasSprite;

import java.util.List;
import java.util.Optional;

public class MolangParticleInstance extends TextureSheetParticle implements IMolangParticleInstance {
    protected final ParticlePreset preset;
    protected ParticleVariableTable vars;
    protected final float invOx;
    protected final float invOy;

    protected Vector3f acceleration = new Vector3f();
    protected Vector3f facingDirection = new Vector3f();
    protected Vector3f initialSpeed = new Vector3f();
    protected float xRot;
    protected float yRot;
    protected float xRotO;
    protected float yRotO;
    protected float rolld;
    protected boolean hasCollision;
    protected float collisionDrag;
    protected float coefficientOfRestitution;
    protected boolean expireOnContact;

    protected final float particleRandom1;
    protected final float particleRandom2;
    protected final float particleRandom3;
    protected final float particleRandom4;
    protected List<IParticleComponent> components;
    protected ParticleEmitter emitter;

    protected final float scaleU;
    protected final float scaleV;
    protected float[] billboardSize = new float[2];
    protected float[] uvSize;
    protected float[] uvStep;
    protected int maxFrame = 1;
    protected int currentFrame;
    protected float[] UV;

    protected boolean insideKillPlane;
    protected ParticleGroup particleGroup;
    protected int lastTimeline;

    public MolangParticleInstance(ParticlePreset preset, ClientLevel level, double x, double y, double z, ExtendMutableSpriteSet sprites) {
        super(level, x, y, z);
        this.friction = 1.0F;
        this.quadSize = 0; // as collision radius
        this.preset = preset;
        setSprite(sprites.get(preset.effect.description.parameters().getTextureIndex()));
        this.invOx = ((IPSTextureAtlasSprite) sprite).particlestorm$getInvOx();
        this.invOy = ((IPSTextureAtlasSprite) sprite).particlestorm$getInvOy();
        this.scaleU = sprite.contents().width() * preset.invTextureWidth;
        this.scaleV = sprite.contents().height() * preset.invTextureHeight;

        RandomSource random = level.getRandom();
        this.particleRandom1 = random.nextFloat();
        this.particleRandom2 = random.nextFloat();
        this.particleRandom3 = random.nextFloat();
        this.particleRandom4 = random.nextFloat();
    }

    @Override
    public int getAge() {
        return age;
    }

    @Override
    public void setEmitter(ParticleEmitter emitter) {
        this.emitter = emitter;
        this.vars = new ParticleVariableTable(preset.vars, emitter.vars);
    }

    @Override
    public ParticlePreset getPreset() {
        return preset;
    }

    @Override
    public TextureAtlasSprite getSprite() {
        return sprite;
    }

    @Override
    public Vector3f getAcceleration() {
        return acceleration;
    }

    @Override
    public Vector3f getFacingDirection() {
        return facingDirection;
    }

    @Override
    public Vector3f getInitialSpeed() {
        return initialSpeed;
    }

    @Override
    public void setXRot(float x, boolean o) {
        this.xRot = x;
        if (o) {
            this.xRotO = x;
        }
    }

    @Override
    public void setYRot(float y, boolean o) {
        this.yRot = y;
        if (o) {
            this.yRotO = y;
        }
    }

    @Override
    public void setZRot(float z, boolean o) {
        this.roll = z;
        if (o) {
            this.oRoll = z;
        }
    }

    @Override
    public void setZRotD(float delta) {
        this.rolld = delta;
    }

    @Override
    public float getZRotD() {
        return rolld;
    }

    @Override
    public void setCollisionDrag(float drag) {
        this.collisionDrag = drag;
    }

    @Override
    public void setCoefficientOfRestitution(float coefficient) {
        this.coefficientOfRestitution = coefficient;
    }

    @Override
    public void setExpireOnContact(boolean b) {
        this.expireOnContact = b;
    }

    @Override
    public void setCollisionRadius(float radius) {
        this.quadSize = radius;
    }

    @Override
    public float getCollisionRadius() {
        return quadSize;
    }

    @Override
    public void setComponents(List<IParticleComponent> components) {
        this.components = components;
    }

    @Override
    public float getScaleU() {
        return scaleU;
    }

    @Override
    public float getScaleV() {
        return scaleV;
    }

    @Override
    public void setBillboardSize(float[] size) {
        this.billboardSize = size;
    }

    @Override
    public void setUvSize(float[] size) {
        this.uvSize = size;
    }

    @Override
    public float[] getUvSize() {
        return uvSize;
    }

    @Override
    public void setUvStep(float[] step) {
        this.uvStep = step;
    }

    @Override
    public float[] getUvStep() {
        return uvStep;
    }

    @Override
    public void setMaxFrame(int frame) {
        this.maxFrame = frame;
    }

    @Override
    public int getMaxFrame() {
        return maxFrame;
    }

    @Override
    public void setCurrentFrame(int frame) {
        this.currentFrame = frame;
    }

    @Override
    public int getCurrentFrame() {
        return currentFrame;
    }

    @Override
    public void setInsideKillPlane(boolean b) {
        this.insideKillPlane = b;
    }

    @Override
    public boolean isInsideKillPlane() {
        return insideKillPlane;
    }

    @Override
    public void setParticleGroup(ParticleGroup group) {
        this.particleGroup = group;
    }

    @Override
    public void setLastTimeline(int last) {
        this.lastTimeline = last;
    }

    @Override
    public int getLastTimeline() {
        return lastTimeline;
    }

    @Override
    public double getXd() {
        return xd;
    }

    @Override
    public double getYd() {
        return yd;
    }

    @Override
    public double getZd() {
        return zd;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getZ() {
        return z;
    }

    @Override
    public void setPos(double x, double y, double z, boolean o) {
        setPos(x, y, z);
        if (o) {
            this.xo = x;
            this.yo = y;
            this.zo = z;
        }
    }

    @Override
    public void setColor(float red, float green, float blue, float alpha) {
        super.setColor(red, green, blue);
        super.setAlpha(alpha);
    }

    @Override
    public void setUV(float u, float v, float w, float h) {
        if (UV == null) this.UV = new float[4];
        this.UV[0] = u * invOx;
        this.UV[1] = v * invOy;
        this.UV[2] = (u + w) * invOx;
        this.UV[3] = (v + h) * invOy;
    }

    @Override
    public void setCollision(boolean bool) {
        this.hasCollision = bool;
    }

    @Override
    public void discard() {
        remove();
    }

    @Override
    public boolean isDiscarded() {
        return removed;
    }

    @Override
    public VariableTable getVars() {
        return vars;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public float getRandom1() {
        return particleRandom1;
    }

    @Override
    public float getRandom2() {
        return particleRandom2;
    }

    @Override
    public float getRandom3() {
        return particleRandom3;
    }

    @Override
    public float getRandom4() {
        return particleRandom4;
    }

    @Override
    public ParticleEmitter getEmitter() {
        return emitter;
    }

    @Override
    protected float getU0() {
        return UV == null ? super.getU0() : UV[0];
    }

    @Override
    protected float getV0() {
        return UV == null ? super.getV0() : UV[1];
    }

    @Override
    protected float getU1() {
        return UV == null ? super.getU1() : UV[2];
    }

    @Override
    protected float getV1() {
        return UV == null ? super.getV1() : UV[3];
    }

    @Override
    public void tick() {
        super.tick();
        this.xRotO = xRot;
        this.yRotO = yRot;
        this.oRoll = roll;
        this.roll = roll + rolld;
        for (IParticleComponent component : components) {
            component.update(this);
        }
    }

    protected static final Quaternionf worldRot = new Quaternionf();
    protected static final Quaternionf localRot = new Quaternionf();
    protected static final Vector3f pos = new Vector3f();

    // 在render前调用
    @Override
    public boolean isVisible(Camera camera, Frustum frustum, float partialTick) {
        Vec3 camPos = camera.getPosition();
        if (emitter.isLocalSpace()) {
            emitter.local2World(pos.set(
                    (float) Mth.lerp(partialTick, xo, x),
                    (float) Mth.lerp(partialTick, yo, y),
                    (float) Mth.lerp(partialTick, zo, z)
            ), partialTick);
            float size = Math.max(billboardSize[0], billboardSize[1]);
            boolean inFrustum = frustum.cubeInFrustum(
                    pos.x - size,
                    pos.y - size,
                    pos.z - size,
                    pos.x + size,
                    pos.y + size,
                    pos.z + size
            );
            pos.sub((float) camPos.x, (float) camPos.y, (float) camPos.z);
            return inFrustum;
        }
        pos.set(
                (float) (Mth.lerp(partialTick, xo, x) - camPos.x),
                (float) (Mth.lerp(partialTick, yo, y) - camPos.y) + 1.0e-4f, // add an epsilon to fix the z-flash
                (float) (Mth.lerp(partialTick, zo, z) - camPos.z)
        );
        return IMolangParticleInstance.super.isVisible(camera, frustum, partialTick);
    }

    // 在isVisible后调用
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        getFacingCameraMode().setRotation(this, worldRot, camera, partialTicks);
        if (xRot != 0.0F) worldRot.rotateX(Mth.lerp(partialTicks, xRotO, xRot));
        if (yRot != 0.0F) worldRot.rotateY(Mth.lerp(partialTicks, yRotO, yRot));
        if (roll != 0.0F) worldRot.rotateZ(Mth.lerp(partialTicks, oRoll, roll));
        if (emitter.isLocalSpace() && emitter.getPreset().localRotation) {
            emitter.getLocalSpace().getNormalizedRotation(localRot);
            worldRot.premul(localRot);
        }
        renderRotatedQuad(buffer, pos.x, pos.y, pos.z, partialTicks);
    }

    protected void renderRotatedQuad(VertexConsumer buffer, float x, float y, float z, float partialTicks) {
        float u0 = getU0();
        float u1 = getU1();
        float v0 = getV0();
        float v1 = getV1();
        int color = getLightColor(partialTicks);
        renderVertex(buffer, x, y, z, -1.0F, -1.0F, u0, v0, color);
        renderVertex(buffer, x, y, z, -1.0F, 1.0F, u0, v1, color);
        renderVertex(buffer, x, y, z, 1.0F, 1.0F, u1, v1, color);
        renderVertex(buffer, x, y, z, 1.0F, -1.0F, u1, v0, color);
    }

    protected void renderVertex(
            VertexConsumer buffer,
            float x,
            float y,
            float z,
            float xOffset,
            float yOffset,
            float u,
            float v,
            int packedLight
    ) {
        pos.set(xOffset * billboardSize[0], yOffset * billboardSize[1], 0.0F).rotate(worldRot).add(x, y, z);
        buffer.vertex(pos.x(), pos.y(), pos.z()).uv(u, v).color(rCol, gCol, bCol, alpha).uv2(packedLight).endVertex();
    }

    @Override
    public void move(double x, double y, double z) {
        if (stoppedByCollision) {
            collisionEvent();
            return;
        }

        double d0 = x;
        double d1 = y;
        double d2 = z;
        if (hasPhysics && hasCollision && (x != 0.0 || y != 0.0 || z != 0.0) && Mth.lengthSquared(x, y, z) < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
            AABB aabb = getBoundingBox();
            if (emitter.isLocalSpace()) {
                emitter.local2World(pos.set(aabb.minX, aabb.minY, aabb.minZ), 1);
                float mx = pos.x;
                float my = pos.y;
                float mz = pos.z;
                emitter.local2World(pos.set(aabb.maxX, aabb.maxY, aabb.maxZ), 1);
                aabb = new AABB(mx, my, mz, pos.x, pos.y, pos.z);
            }
            Vec3 vec3 = Entity.collideBoundingBox(null, new Vec3(x, y, z), aabb, level, List.of());
            if (x != vec3.x) {
                this.xd = -Mth.sign(xd) * (Math.abs(xd) - collisionDrag) * coefficientOfRestitution;
            }
            if (y != vec3.y) {
                this.yd *= -coefficientOfRestitution;
            }
            if (z != vec3.z) {
                this.zd = -Mth.sign(zd) * (Math.abs(zd) - collisionDrag) * coefficientOfRestitution;
            }
            x = vec3.x;
            y = vec3.y;
            z = vec3.z;
        }

        if (x != 0.0 || y != 0.0 || z != 0.0) {
            moveDirectly(x, y, z);
        }

        if (Math.abs(d1) >= Mth.EPSILON && Math.abs(y) < Mth.EPSILON) {
            this.stoppedByCollision = true;
        }

        if (hasPhysics && hasCollision) {
            this.onGround = d1 != y && d1 < 0.0;

            if (onGround || (d0 != x || d2 != z)) {
                collisionEvent();
                if (expireOnContact) {
                    remove();
                }
            }
        }
    }

    protected void collisionEvent() {
        if (preset.collisionEvents.isEmpty()) return;
        for (ParticleMotionCollision.Event event : preset.collisionEvents) {
            float tickSpeed = event.minSpeed() * getInvTickRate();
            if (tickSpeed * tickSpeed < Mth.lengthSquared(xd, yd, zd)) {
                for (IEventNode node : preset.effect.events.get(event.event()).values()) {
                    node.execute(this);
                }
            }
        }
    }

    @Override
    public void setParticleSpeed(double xd, double yd, double zd) {
        if (stoppedByCollision) return;
        super.setParticleSpeed(xd, yd, zd);
    }

    @Override
    public void remove() {
        if (preset.lifeTimeEvents != null) {
            preset.lifeTimeEvents.onExpiration(this);
        }
        super.remove();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return preset.renderType;
    }

    public FaceCameraMode getFacingCameraMode() {
        return preset.facingCameraMode;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return preset.environmentLighting ? super.getLightColor(partialTick) : 0xF000F0;
    }

    @Override
    public Optional<ParticleGroup> getParticleGroup() {
        return Optional.ofNullable(particleGroup);
    }
}

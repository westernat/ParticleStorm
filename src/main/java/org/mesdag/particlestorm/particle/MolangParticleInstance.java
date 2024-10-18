package org.mesdag.particlestorm.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.mesdag.particlestorm.GameClient;
import org.mesdag.particlestorm.data.component.IParticleComponent;
import org.mesdag.particlestorm.data.component.ParticleMotionCollision;
import org.mesdag.particlestorm.data.event.IEventNode;
import org.mesdag.particlestorm.data.molang.MolangInstance;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.mixinauxi.ITextureAtlasSprite;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class MolangParticleInstance extends TextureSheetParticle implements MolangInstance {
    public static final int FULL_LIGHT = 0xF000F0;
    public final RandomSource random;
    public final ParticleDetail detail;
    private final VariableTable variableTable;
    public final float originX;
    public final float originY;

    public Vector3f initialSpeed = new Vector3f();
    public float xRot = 0.0F;
    public float yRot = 0.0F;
    protected float xRotO = 0.0F;
    protected float yRotO = 0.0F;
    public float rolld = 0.0F;
    private boolean hasCollision = false;
    public float collisionDrag = 0.0F;
    public float coefficientOfRestitution = 0.0F;
    public boolean expireOnContact = false;

    protected final double particleRandom1;
    protected final double particleRandom2;
    protected final double particleRandom3;
    protected final double particleRandom4;
    public List<IParticleComponent> components;
    public ParticleEmitter emitter;
    public boolean motionDynamic = false;

    public float[] billboardSize = new float[2];
    public float[] uvSize;
    public float[] uvStep;
    public int maxFrame = 1;
    public int currentFrame = 0;
    public float[] UV;

    public boolean insideKillPlane;
    public ParticleGroup particleGroup;
    public int lastTimeline = 0;

    public MolangParticleInstance(ParticleDetail detail, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, ExtendMutableSpriteSet sprites) {
        super(level, x, y, z);
        this.friction = 1.0F;
        this.random = level.getRandom();
        this.detail = detail;
        this.variableTable = new VariableTable(detail.variableTable);
        setSprite(sprites.get(detail.effect.description.parameters().getTextureIndex()));
        this.originX = ((ITextureAtlasSprite) sprite).particlestorm$getOriginX();
        this.originY = ((ITextureAtlasSprite) sprite).particlestorm$getOriginY();

        this.particleRandom1 = random.nextDouble();
        this.particleRandom2 = random.nextDouble();
        this.particleRandom3 = random.nextDouble();
        this.particleRandom4 = random.nextDouble();
    }

    public double getXd() {
        return xd;
    }

    public double getYd() {
        return yd;
    }

    public double getZd() {
        return zd;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public void setPosO(double x, double y, double z) {
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    public void setRoll(float roll) {
        this.roll = roll;
    }

    public void setColor(float red, float green, float blue, float alpha) {
        super.setColor(red, green, blue);
        super.setAlpha(alpha);
    }

    public void setUV(float u, float v, float w, float h) {
        if (UV == null) this.UV = new float[4];
        this.UV[0] = u / originX;
        this.UV[1] = v / originY;
        this.UV[2] = (u + w) / originX;
        this.UV[3] = (v + h) / originY;
    }

    public void setCollision(boolean bool) {
        this.hasCollision = bool;
    }

    @Override
    public VariableTable getVariableTable() {
        return variableTable;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    public float tickAge() {
        return age * emitter.invTickRate;
    }

    @Override
    public float tickLifetime() {
        return lifetime * emitter.invTickRate;
    }

    @Override
    public double getRandom1() {
        return particleRandom1;
    }

    @Override
    public double getRandom2() {
        return particleRandom2;
    }

    @Override
    public double getRandom3() {
        return particleRandom3;
    }

    @Override
    public double getRandom4() {
        return particleRandom4;
    }

    @Override
    public ResourceLocation getIdentity() {
        return detail.effect.description.identifier();
    }

    @Override
    public Vec3 getPosition() {
        return getPos();
    }

    @Override
    public Entity getAttachedEntity() {
        return emitter.attached;
    }

    @Override
    public float getInvTickRate() {
        return emitter.invTickRate;
    }

    @Override
    public ParticleEmitter getEmitter() {
        return emitter;
    }

    public TextureAtlasSprite getSprite() {
        return sprite;
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

    public int getAge() {
        return age;
    }

    @Override
    public void tick() {
        super.tick();
        this.xRotO = xRot;
        this.yRotO = yRot;
        this.oRoll = roll;
        this.roll = roll + rolld * Mth.TWO_PI;
        for (IParticleComponent component : components) {
            component.update(this);
        }
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera renderInfo, float partialTicks) {
        Quaternionf quaternionf = new Quaternionf();
        getFacingCameraMode().setRotation((float) x, (float) y, (float) z, quaternionf, renderInfo, partialTicks);
        if (this.roll != 0.0F) {
            quaternionf.rotateZ(Mth.lerp(partialTicks, this.oRoll, this.roll));
        }
        renderRotatedQuad(buffer, renderInfo, quaternionf, partialTicks);
    }

    @Override
    protected void renderRotatedQuad(@NotNull VertexConsumer buffer, @NotNull Camera camera, @NotNull Quaternionf quaternion, float partialTicks) {
        if (xRot != 0.0F) quaternion.rotateX(Mth.lerp(partialTicks, xRotO, xRot));
        if (yRot != 0.0F) quaternion.rotateY(Mth.lerp(partialTicks, yRotO, yRot));
        super.renderRotatedQuad(buffer, camera, quaternion, partialTicks);
    }

    @Override
    protected void renderVertex(@NotNull VertexConsumer buffer, @NotNull Quaternionf quaternion, float x, float y, float z, float xOffset, float yOffset, float quadSize, float u, float v, int packedLight) {
        Vector3f vector3f = new Vector3f(xOffset * billboardSize[0], yOffset * billboardSize[1], 0.0F).rotate(quaternion).add(x, y, z);
        buffer.addVertex(vector3f.x(), vector3f.y(), vector3f.z()).setUv(u, v).setColor(rCol, gCol, bCol, alpha).setLight(packedLight);
    }

    public void updateBillboardRotation(double xdSqr, double zdSqr) {
        double d0 = Math.sqrt(xdSqr + zdSqr);
        this.xRot = (float) (Mth.atan2(yd, d0) * Mth.RAD_TO_DEG);
        this.yRot = (float) (Mth.atan2(xd, zd) * Mth.RAD_TO_DEG);
    }

    public void moveDirectly(double x, double y, double z) {
        setBoundingBox(getBoundingBox().move(x, y, z));
        setLocationFromBoundingbox();
    }

    @Override
    public void move(double x, double y, double z) {
        if (!stoppedByCollision) {
            double d0 = x;
            double d1 = y;
            double d2 = z;
            if (hasPhysics && (x != 0.0 || y != 0.0 || z != 0.0) && x * x + y * y + z * z < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
                Vec3 vec3 = Entity.collideBoundingBox(null, new Vec3(x, y, z), getBoundingBox(), level, List.of());
                if (hasCollision) {
                    if (x != vec3.x) {
                        this.xd = -Mth.sign(xd) * Mth.clamp(Math.abs(xd) - collisionDrag, 0.0, Double.MAX_VALUE);
                    }
                    if (y != vec3.y) this.yd *= -coefficientOfRestitution;
                    if (z != vec3.z) this.zd = -Mth.sign(zd) * Mth.clamp(Math.abs(zd) - collisionDrag, 0.0, Double.MAX_VALUE);
                }
                x = vec3.x;
                y = vec3.y;
                z = vec3.z;
            }

            if (x != 0.0 || y != 0.0 || z != 0.0) {
                moveDirectly(x, y, z);
            }

            if (Math.abs(d1) >= 1.0E-5F && Math.abs(y) < 1.0E-5F) {
                this.stoppedByCollision = true;
            }

            this.onGround = d1 != y && d1 < 0.0;
            boolean collided = false;
            if (d0 != x) {
                collided = true;
                if (!hasCollision) this.xd = 0.0;
            }
            if (d2 != z) {
                collided = true;
                if (!hasCollision) this.zd = 0.0;
            }

            if (onGround || collided) {
                if (!detail.collisionEvents.isEmpty()) {
                    Map<String, Map<String, IEventNode>> events = detail.effect.events;
                    for (ParticleMotionCollision.Event event : detail.collisionEvents) {
                        float tickSpeed = event.minSpeed() * getInvTickRate();
                        if (tickSpeed * tickSpeed < xd * xd + yd * yd + zd * zd) {
                            events.get(event.event()).forEach((name, node) -> node.execute(this));
                        }
                    }
                }
                if (expireOnContact) {
                    remove();
                }
            }
        }
    }

    @Override
    public void remove() {
        if (detail.lifeTimeEvents != null) {
            detail.lifeTimeEvents.onExpiration(this);
        }
        super.remove();
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return detail.renderType;
    }

    @Override
    public @NotNull FaceCameraMode getFacingCameraMode() {
        return detail.facingCameraMode;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return detail.environmentLighting ? super.getLightColor(partialTick) : FULL_LIGHT;
    }

    @Override
    public @NotNull Optional<ParticleGroup> getParticleGroup() {
        return Optional.ofNullable(particleGroup);
    }

    public static class Provider implements ParticleProvider<MolangParticleOption> {
        private final ExtendMutableSpriteSet sprites;

        public Provider(ExtendMutableSpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public TextureSheetParticle createParticle(@NotNull MolangParticleOption option, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new MolangParticleInstance(GameClient.LOADER.ID_2_PARTICLE.get(option.getId()), level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}

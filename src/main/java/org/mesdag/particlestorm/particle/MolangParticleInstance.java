package org.mesdag.particlestorm.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleLimit;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.mesdag.particlestorm.PSDiagnostics;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.component.ParticleMotionCollision;
import org.mesdag.particlestorm.data.event.EventResolver;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.mixed.ITextureAtlasSprite;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MolangParticleInstance extends SingleQuadParticle implements IMolangParticleInstance {
    public static final int FULL_LIGHT = 0xF000F0;
    private static final float MIN_RENDER_SIZE = 1.0E-4F;
    private static final int MAX_RECTANGLE_SEGMENTS = 24;

    protected final ParticlePreset preset;
    protected ParticleVariableTable vars;
    protected final float originX;
    protected final float originY;

    protected Vector3f acceleration = new Vector3f();
    protected Vector3f facingDirection = new Vector3f();
    protected Vector3f initialSpeed = new Vector3f();
    protected final Vector3f renderPosition = new Vector3f();
    protected float xRot = 0.0F;
    protected float yRot = 0.0F;
    protected float xRotO = 0.0F;
    protected float yRotO = 0.0F;
    protected float rolld = 0.0F;
    protected boolean hasCollision = false;
    protected float collisionDrag = 0.0F;
    protected float coefficientOfRestitution = 0.0F;
    protected float collisionRadius = 0.0F;
    protected boolean expireOnContact = false;

    protected final double particleRandom1;
    protected final double particleRandom2;
    protected final double particleRandom3;
    protected final double particleRandom4;
    protected List<IParticleComponent> components = List.of();
    protected ParticleEmitter emitter;

    protected final float scaleU;
    protected final float scaleV;
    protected float[] billboardSize = new float[2];
    protected float[] uvSize;
    protected float[] uvStep;
    protected int maxFrame = 1;
    protected int currentFrame = 0;
    protected float[] UV;

    protected boolean insideKillPlane;
    protected ParticleLimit particleGroup;
    protected int lastTimeline = 0;

    public MolangParticleInstance(ParticlePreset preset, ClientLevel level, double x, double y, double z, RandomSource random) {
        super(level, x, y, z, preset.effect.description.parameters().getTexture());
        this.friction = 1.0F;
        this.preset = preset;
        this.originX = ((ITextureAtlasSprite) sprite).particlestorm$getOriginX();
        this.originY = ((ITextureAtlasSprite) sprite).particlestorm$getOriginY();
        this.scaleU = sprite.contents().width() * preset.invTextureWidth;
        this.scaleV = sprite.contents().height() * preset.invTextureHeight;

        this.particleRandom1 = random.nextDouble();
        this.particleRandom2 = random.nextDouble();
        this.particleRandom3 = random.nextDouble();
        this.particleRandom4 = random.nextDouble();
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
    public void setXRot(float x) {
        this.xRot = x;
    }

    @Override
    public void setYRot(float y) {
        this.yRot = y;
    }

    @Override
    public void setZRot(float z) {
        this.roll = z;
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
        this.collisionRadius = radius;
    }

    @Override
    public float getCollisionRadius() {
        return collisionRadius;
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
        this.maxFrame = Math.max(frame, 1);
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
    public void setParticleGroup(ParticleLimit group) {
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
    public void setPosO(double x, double y, double z) {
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    public void setColor(float red, float green, float blue, float alpha) {
        super.setColor(red, green, blue);
        super.setAlpha(alpha);
    }

    @Override
    public void setUV(float u, float v, float w, float h) {
        if (UV == null) this.UV = new float[4];
        this.UV[0] = u / originX;
        this.UV[1] = v / originY;
        this.UV[2] = (u + w) / originX;
        this.UV[3] = (v + h) / originY;
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
        PSDiagnostics.infoFirstN("particle-tick:" + (emitter == null ? "none" : emitter.id + ":" + emitter.particleId), 8, "particle tick runtimeId={} state={}",
                emitter == null ? -1 : emitter.id,
                diagnosticSummary()
        );
    }

    @Override
    public float getQuadSize(float partialTicks) {
        float width = getBillboardWidth(partialTicks);
        float height = getBillboardHeight(partialTicks);
        float size = Math.max(width, height);
        return size > MIN_RENDER_SIZE ? size : super.getQuadSize(partialTicks);
    }

    @Override
    public void extract(@NotNull QuadParticleRenderState state, @NotNull Camera camera, float partialTicks) {
        Quaternionf quaternionf = new Quaternionf();
        getFacingCameraMode().setRotation(this, quaternionf, camera, partialTicks);
        if (xRot != 0.0F) quaternionf.rotateX(Mth.lerp(partialTicks, xRotO, xRot));
        if (yRot != 0.0F) quaternionf.rotateY(Mth.lerp(partialTicks, yRotO, yRot));
        if (roll != 0.0F) quaternionf.rotateZ(Mth.lerp(partialTicks, oRoll, roll));

        if (emitter != null && emitter.isLocalSpace()) {
            Vec3 camPos = camera.position();
            renderPosition.set(
                    (float) Mth.lerp((double) partialTicks, xo, x),
                    (float) Mth.lerp((double) partialTicks, yo, y),
                    (float) Mth.lerp((double) partialTicks, zo, z)
            );
            emitter.local2World(renderPosition, partialTicks);
            renderPosition.sub((float) camPos.x, (float) camPos.y, (float) camPos.z);
            extractRotatedQuad(state, quaternionf, renderPosition.x, renderPosition.y, renderPosition.z, partialTicks);
            return;
        }
        Vec3 camPos = camera.position();
        renderPosition.set(
                (float) (Mth.lerp(partialTicks, xo, x) - camPos.x),
                (float) (Mth.lerp(partialTicks, yo, y) - camPos.y) + MIN_RENDER_SIZE,
                (float) (Mth.lerp(partialTicks, zo, z) - camPos.z)
        );
        extractRotatedQuad(state, quaternionf, renderPosition.x, renderPosition.y, renderPosition.z, partialTicks);
    }

    public boolean particlestorm$isVisible(Frustum frustum, float partialTick) {
        if (emitter != null && emitter.isLocalSpace()) {
            renderPosition.set(
                    (float) Mth.lerp((double) partialTick, xo, x),
                    (float) Mth.lerp((double) partialTick, yo, y),
                    (float) Mth.lerp((double) partialTick, zo, z)
            );
            emitter.local2World(renderPosition, partialTick);
            float size = Math.max(getBillboardWidth(partialTick), getBillboardHeight(partialTick));
            return frustum.isVisible(new AABB(
                    renderPosition.x - size,
                    renderPosition.y - size,
                    renderPosition.z - size,
                    renderPosition.x + size,
                    renderPosition.y + size,
                    renderPosition.z + size
            ));
        }
        return frustum.pointInFrustum(x, y, z);
    }

    @Override
    protected void extractRotatedQuad(QuadParticleRenderState state, Quaternionf orientation, float x, float y, float z, float partialTick) {
        float width = getBillboardWidth(partialTick);
        float height = getBillboardHeight(partialTick);
        int color = ARGB.colorFromFloat(this.alpha, this.rCol, this.gCol, this.bCol);
        int light = getLightCoords(partialTick);

        if (width <= MIN_RENDER_SIZE || height <= MIN_RENDER_SIZE || Math.abs(width - height) <= MIN_RENDER_SIZE) {
            float size = Math.max(width, height);
            if (size <= MIN_RENDER_SIZE) {
                size = super.getQuadSize(partialTick);
            }
            addBillboardFace(state, orientation, x, y, z, size, getU0(), getU1(), getV0(), getV1(), color, light);
            return;
        }

        addSegmentedRectangularQuad(state, orientation, x, y, z, width, height, color, light);
    }

    private float getBillboardWidth(float partialTick) {
        return billboardSize == null || billboardSize.length < 2 ? super.getQuadSize(partialTick) : Math.abs(billboardSize[0]);
    }

    private float getBillboardHeight(float partialTick) {
        return billboardSize == null || billboardSize.length < 2 ? super.getQuadSize(partialTick) : Math.abs(billboardSize[1]);
    }

    private void addSegmentedRectangularQuad(QuadParticleRenderState state, Quaternionf orientation, float x, float y, float z, float width, float height, int color, int light) {
        boolean splitWidth = width >= height;
        float major = splitWidth ? width : height;
        float minor = splitWidth ? height : width;
        int segments = Math.max(1, Math.min(MAX_RECTANGLE_SEGMENTS, (int) Math.ceil(major / Math.max(minor, MIN_RENDER_SIZE))));
        Vector3f axis = new Vector3f(splitWidth ? 1.0F : 0.0F, splitWidth ? 0.0F : 1.0F, 0.0F).rotate(orientation);

        float u0 = getU0();
        float u1 = getU1();
        float v0 = getV0();
        float v1 = getV1();
        float startOffset = -major + minor;
        float endOffset = major - minor;

        for (int segment = 0; segment < segments; segment++) {
            float segmentStart = (float) segment / segments;
            float segmentEnd = (float) (segment + 1) / segments;
            float offset = segments == 1 ? 0.0F : Mth.lerp((float) segment / (segments - 1), startOffset, endOffset);
            float segmentU0 = splitWidth ? Mth.lerp(segmentStart, u0, u1) : u0;
            float segmentU1 = splitWidth ? Mth.lerp(segmentEnd, u0, u1) : u1;
            float segmentV0 = splitWidth ? v0 : Mth.lerp(segmentStart, v0, v1);
            float segmentV1 = splitWidth ? v1 : Mth.lerp(segmentEnd, v0, v1);

            addBillboardFace(state, orientation, x + axis.x * offset, y + axis.y * offset, z + axis.z * offset, minor, segmentU0, segmentU1, segmentV0, segmentV1, color, light);
        }
    }

    private void addBillboardFace(QuadParticleRenderState state, Quaternionf orientation, float x, float y, float z, float size, float u0, float u1, float v0, float v1, int color, int light) {
        SingleQuadParticle.Layer layer = getLayer();
        state.add(layer, x, y, z, orientation.x, orientation.y, orientation.z, orientation.w, size, u0, u1, v0, v1, color, light);
        Quaternionf back = new Quaternionf(orientation).rotateY(Mth.PI);
        state.add(layer, x, y, z, back.x, back.y, back.z, back.w, size, u1, u0, v0, v1, color, light);
    }

    @Override
    public void move(double x, double y, double z) {
        if (stoppedByCollision) return;
        double d0 = x;
        double d1 = y;
        double d2 = z;
        if (hasPhysics && hasCollision && (x != 0.0 || y != 0.0 || z != 0.0) && x * x + y * y + z * z < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
            AABB aabb = getBoundingBox();
            if (collisionRadius > 0.0F) {
                aabb = aabb.inflate(collisionRadius, 0.0, collisionRadius);
            }
            if (emitter != null && emitter.isLocalSpace()) {
                emitter.local2World(renderPosition.set((float) aabb.minX, (float) aabb.minY, (float) aabb.minZ), 1.0F);
                float minX = renderPosition.x;
                float minY = renderPosition.y;
                float minZ = renderPosition.z;
                emitter.local2World(renderPosition.set((float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ), 1.0F);
                aabb = new AABB(minX, minY, minZ, renderPosition.x, renderPosition.y, renderPosition.z);
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
            boolean collided = d0 != x || d2 != z;

            if (onGround || collided) {
                if (!preset.collisionEvents.isEmpty()) {
                    for (ParticleMotionCollision.Event event : preset.collisionEvents) {
                        float tickSpeed = event.minSpeed() * getInvTickRate();
                        if (tickSpeed * tickSpeed < xd * xd + yd * yd + zd * zd) {
                            for (IEventNode node : EventResolver.resolve(preset.effect.events, event.event()).values()) {
                                node.execute(this);
                            }
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
        if (!removed) {
            if (preset.lifeTimeEvents != null) {
                preset.lifeTimeEvents.onExpiration(this);
            }
        }
        super.remove();
    }

    @Override
    public @NotNull ParticleRenderType getGroup() {
        return preset.renderType == null ? ParticleRenderType.NO_RENDER : ParticleRenderType.SINGLE_QUADS;
    }

    @Override
    protected @NotNull Layer getLayer() {
        return preset.renderType == null ? Layer.OPAQUE : preset.renderType;
    }

    @Override
    public @NotNull FaceCameraMode getFacingCameraMode() {
        return preset.facingCameraMode;
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return preset.environmentLighting ? super.getLightCoords(partialTick) : FULL_LIGHT;
    }

    @Override
    public @NotNull Optional<ParticleLimit> getParticleLimit() {
        return Optional.empty();
    }

    @Override
    public Identifier getIdentity() {
        return preset.effect.description.identifier();
    }

    @Override
    public Vec3 getPosition() {
        return new Vec3(x, y, z);
    }

    public String diagnosticSummary() {
        return "identity=" + getIdentity() +
                ",pos=(" + x + "," + y + "," + z + ")" +
                ",speed=(" + xd + "," + yd + "," + zd + ")" +
                ",age=" + age +
                ",lifetime=" + getLifetime() +
                ",quadSize=" + getQuadSize(0.0F) +
                ",billboardSize=" + Arrays.toString(billboardSize) +
                ",color=(" + rCol + "," + gCol + "," + bCol + "," + alpha + ")" +
                ",spriteSize=" + sprite.contents().width() + "x" + sprite.contents().height() +
                ",atlasSize=" + originX + "x" + originY +
                ",scale=(" + scaleU + "," + scaleV + ")" +
                ",uv=" + Arrays.toString(UV) +
                ",uvSize=" + Arrays.toString(uvSize) +
                ",uvStep=" + Arrays.toString(uvStep) +
                ",frame=" + currentFrame + "/" + maxFrame +
                ",layer=" + getLayer() +
                ",group=" + getGroup() +
                ",motionDynamic=" + preset.motionDynamic +
                ",components=" + components.stream().map(component -> component.getClass().getSimpleName()).toList() +
                ",flags=" + diagnosticFlags();
    }

    private String diagnosticFlags() {
        StringBuilder builder = new StringBuilder();
        appendDiagnosticFlag(builder, getLifetime() <= 0, "lifetime<=0");
        appendDiagnosticFlag(builder, billboardSize == null || billboardSize.length < 2 || billboardSize[0] <= 0.0F || billboardSize[1] <= 0.0F, "size<=0");
        appendDiagnosticFlag(builder, alpha <= 0.0F, "alpha<=0");
        appendDiagnosticFlag(builder, preset.renderType == null, "no_render_type");
        appendDiagnosticFlag(builder, sprite.contents().width() <= 0 || sprite.contents().height() <= 0, "sprite_empty");
        return builder.isEmpty() ? "none" : builder.toString();
    }

    private static void appendDiagnosticFlag(StringBuilder builder, boolean condition, String name) {
        if (!condition) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append('|');
        }
        builder.append(name);
    }

    public static class Provider implements ParticleProvider<MolangParticleOption> {
        @Override
        public @Nullable Particle createParticle(@NotNull MolangParticleOption option, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
            return new MolangParticleInstance(option.getPreset(), level, x, y, z, random);
        }
    }
}

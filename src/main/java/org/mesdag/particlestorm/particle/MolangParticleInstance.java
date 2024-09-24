package org.mesdag.particlestorm.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.mesdag.particlestorm.GameClient;
import org.mesdag.particlestorm.ITextureAtlasSprite;
import org.mesdag.particlestorm.data.component.IParticleComponent;
import org.mesdag.particlestorm.data.molang.VariableTable;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class MolangParticleInstance extends TextureSheetParticle {
    public static final int FULL_LIGHT = 0xF000F0;
    public final RandomSource random;
    public final ParticleDetail detail;
    public final VariableTable variableTable;
    public final float originX;
    public final float originY;

    public float xRot = 0.0F;
    public float yRot = 0.0F;
    protected float xRotO = 0.0F;
    protected float yRotO = 0.0F;
    public float rolld = 0.0F;

    public final double particleRandom1;
    public final double particleRandom2;
    public final double particleRandom3;
    public final double particleRandom4;
    protected final Collection<IParticleComponent> components;

    public float[] billboardSize = new float[2];
    public float[] uvSize;
    public float[] uvStep;
    public int maxFrame = 1;
    public int currentFrame = 1;
    public float[] UV;

    public MolangParticleInstance(ParticleDetail detail, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, ExtendMutableSpriteSet sprites) {
        super(level, x, y, z);
        this.random = level.getRandom();
        this.detail = detail;
        this.variableTable = new VariableTable(detail.variableTable);
        setSprite(sprites.get(detail.effect.getDescription().parameters().getTextureIndex()));
        this.originX = ((ITextureAtlasSprite) sprite).particlestorm$getOriginX();
        this.originY = ((ITextureAtlasSprite) sprite).particlestorm$getOriginY();

        this.particleRandom1 = random.nextDouble();
        this.particleRandom2 = random.nextDouble();
        this.particleRandom3 = random.nextDouble();
        this.particleRandom4 = random.nextDouble();
        this.components = detail.effect.getComponents().values().stream().filter(c -> {
            if (c instanceof IParticleComponent p) {
                p.apply(this);
                return p.requireUpdate();
            }
            return false;
        }).map(c -> (IParticleComponent) c).collect(Collectors.toList());
        detail.assignments.forEach(assignment -> {
            // 重定向，防止污染变量表
            variableTable.setValue(assignment.variable().name(), assignment.variable().get(this));
        });
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

    public void setRoll(float roll) {
        this.roll = roll;
    }

    public void setUV(float u, float v, float w, float h) {
        if (UV == null) this.UV = new float[4];
        this.UV[0] = u / originX;
        this.UV[1] = v / originY;
        this.UV[2] = (u + w) / originX;
        this.UV[3] = (v + h) / originY;
    }

    public Level level() {
        return level;
    }

    public int getAge() {
        return age;
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

    @Override
    public void tick() {
        super.tick();
        this.xRotO = xRot;
        this.yRotO = yRot;
        this.oRoll = roll;
        for (IParticleComponent component : components) {
            component.update(this);
        }
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

    public void updateRotation(double xdSqr, double zdSqr) {
        double d0 = Math.sqrt(xdSqr + zdSqr);
        this.xRot = (float) (Mth.atan2(yd, d0) * Mth.RAD_TO_DEG);
        this.yRot = (float) (Mth.atan2(xd, zd) * Mth.RAD_TO_DEG);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return detail.renderType;
    }

    @Override
    public @NotNull FacingCameraMode getFacingCameraMode() {
        return detail.facingCameraMode;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return detail.environmentLighting ? super.getLightColor(partialTick) : FULL_LIGHT;
    }

    public static class Provider implements ParticleProvider<MolangParticleOption> {
        private final ExtendMutableSpriteSet sprites;
        private ParticleDetail cachedDetail;

        public Provider(ExtendMutableSpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public TextureSheetParticle createParticle(@NotNull MolangParticleOption option, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new MolangParticleInstance(Objects.requireNonNull(GameClient.LOADER.ID_2_DETAIL.get(option.getId())), level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}

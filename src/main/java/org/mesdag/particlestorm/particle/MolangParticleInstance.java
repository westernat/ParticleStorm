package org.mesdag.particlestorm.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.mesdag.particlestorm.GameClient;
import org.mesdag.particlestorm.data.component.IParticleComponent;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class MolangParticleInstance extends TextureSheetParticle {
    public static final int FULL_LIGHT = 0xF000F0;
    public final ParticleDetail detail;
    protected final Collection<IParticleComponent> components;
    public float xRot = 0.0F;
    public float yRot = 0.0F;
    protected float xRotO = 0.0F;
    protected float yRotO = 0.0F;
    public float rolld = 0.0F;
    public float[] billboardSize = new float[0];

    public float[] baseUV = new float[0];
    public float[] uvSize = new float[0];
    public float[] uvStep = new float[0];

    public MolangParticleInstance(MolangParticleOption option, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, ExtendMutableSpriteSet sprites) {
        super(level, x, y, z);
        this.detail = Objects.requireNonNull(GameClient.LOADER.ID_2_DETAIL.get(option.getId()));
        this.components = detail.effect.getComponents().values().stream().filter(c -> {
            if (c instanceof IParticleComponent p) {
                p.apply(this);
                return p.requireUpdate();
            }
            return false;
        }).map(c -> (IParticleComponent) c).collect(Collectors.toList());
        setSprite(sprites.get(detail.effect.getDescription().parameters().getTextureIndex()));
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
        Vector3f vector3f = new Vector3f(xOffset, yOffset, 0.0F).rotate(quaternion).add(x, y, z);
        if (billboardSize[0] != 1.0F || billboardSize[1] != 1.0F) vector3f.mul(billboardSize[0], billboardSize[1], 1.0F);
        else vector3f.mul(quadSize);
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

        public Provider(ExtendMutableSpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public TextureSheetParticle createParticle(@NotNull MolangParticleOption option, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            MolangParticleInstance instance = new MolangParticleInstance(option, level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
            instance.setSprite(sprites.get(-1));
            return instance;
        }
    }
}

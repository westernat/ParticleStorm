package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.List;

/// Particles that cross this plane expire.
///
/// The plane is relative to the emitter, but oriented in world space.
///
/// The four parameters are the usual 4 elements of a plane equation.
///
/// `A*x + B*y + C*z + D = 0` with the parameters being `[ A, B, C, D ]`
public final class ParticleLifetimeKillPlane implements IParticleComponent {
    public static final ResourceLocation ID = ResourceLocation.withDefaultNamespace("particle_kill_plane");
    public static final Codec<ParticleLifetimeKillPlane> CODEC = Codec.FLOAT.listOf().xmap(
            floats -> new ParticleLifetimeKillPlane(floats.get(0), floats.get(1), floats.get(2), floats.get(3)),
            plane -> List.of(plane.A, plane.B, plane.C, plane.D)
    );
    /// 计算粒子到平面的有符号距离时复用的临时向量（只在客户端主线程调用）。
    private static final Vector3f WORLD_POS = new Vector3f();
    public final float A;
    public final float B;
    public final float C;
    public final float D;

    private final float normalLengthSqr;

    public ParticleLifetimeKillPlane(float A, float B, float C, float D) {
        this.A = A;
        this.B = B;
        this.C = C;
        this.D = D;

        this.normalLengthSqr = A * A + B * B + C * C;
    }

    /// `A=B=C=0` 时方程退化，没有可用的平面（不做任何剔除）。
    public boolean isDegenerate() {
        return this.normalLengthSqr < 1.0E-12F;
    }

    @Override
    public void update(IMolangParticleInstance instance) {
        if (isDegenerate()) return;
        if ((distanceToPlane(instance) < 0.0F) != instance.isInsideKillPlane()) {
            instance.self().remove();
        }
    }

    @Override
    public void apply(IMolangParticleInstance instance) {
        instance.setInsideKillPlane(!isDegenerate() && distanceToPlane(instance) < 0.0F);
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    /// 把粒子位置代入 `A*x + B*y + C*z + D`（符号即所在侧）。
    ///
    /// 平面相对发射器定义，所以要先取粒子的**世界坐标**再减去发射器位置；
    /// 本地空间粒子的 `getX/Y/Z` 是发射器本地坐标，必须经 `getWorldPosition` 变换。
    private float distanceToPlane(IMolangParticleInstance instance) {
        double x = instance.getX();
        double y = instance.getY();
        double z = instance.getZ();
        if (instance.getEmitter().isLocalSpace()) {
            WORLD_POS.set((float) x, (float) y, (float) z);
            instance.getWorldPosition(WORLD_POS, 1.0F);
            x = WORLD_POS.x;
            y = WORLD_POS.y;
            z = WORLD_POS.z;
        }
        Vec3 emitterPos = instance.getEmitter().getPosition();
        return (float) (A * (x - emitterPos.x) + B * (y - emitterPos.y) + C * (z - emitterPos.z) + D);
    }

    @Override
    public Codec<ParticleLifetimeKillPlane> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of();
    }

    @Override
    public String toString() {
        return "ParticleLifetimeKillPlane[" +
                "A=" + A + ", " +
                "B=" + B + ", " +
                "C=" + C + ", " +
                "D=" + D + ']';
    }
}

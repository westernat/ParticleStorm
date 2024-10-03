package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

import java.util.List;

/**
 * Particles that cross this plane expire.<p>
 * The plane is relative to the emitter, but oriented in world space.<p>
 * The four parameters are the usual 4 elements of a plane equation.<p>
 * A*x + B*y + C*z + D = 0 with the parameters being [ A, B, C, D ]
 */
public final class ParticleLifetimeKillPlane implements IParticleComponent {
    public static final Codec<ParticleLifetimeKillPlane> CODEC = Codec.list(Codec.FLOAT, 4, 4).xmap(
            floats -> new ParticleLifetimeKillPlane(floats.getFirst(), floats.get(1), floats.get(2), floats.get(3)),
            plane -> List.of(plane.A, plane.B, plane.C, plane.D)
    );
    public final float A;
    public final float B;
    public final float C;
    public final float D;

    private final float killDistanceSqr;

    public ParticleLifetimeKillPlane(float A, float B, float C, float D) {
        this.A = A;
        this.B = B;
        this.C = C;
        this.D = D;

        this.killDistanceSqr = D * D / (A * A + B * B + C * C);
    }

    @Override
    public void update(MolangParticleInstance instance) {
        if (instance.motionDynamic) return;
        if (distanceSqr(instance.getX(), instance.getY(), instance.getZ()) > killDistanceSqr == instance.insideKillPlane) {
            instance.remove();
        }
    }

    @Override
    public void apply(MolangParticleInstance instance) {
        instance.insideKillPlane = distanceSqr(instance.getX(), instance.getY(), instance.getZ()) < killDistanceSqr;
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    private float distanceSqr(double x, double y, double z) {
        return (float) (x * x + y * y + z * z);
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

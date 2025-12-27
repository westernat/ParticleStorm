package org.mesdag.particlestorm.data.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.mesdag.particlestorm.api.Deserializer;
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
    public void update(IMolangParticleInstance instance) {
        if (instance.getPreset().motionDynamic) return;
        if (distanceSqr(instance.getX(), instance.getY(), instance.getZ()) > killDistanceSqr == instance.isInsideKillPlane()) {
            instance.self().remove();
        }
    }

    @Override
    public void apply(IMolangParticleInstance instance) {
        instance.setInsideKillPlane(distanceSqr(instance.getX(), instance.getY(), instance.getZ()) < killDistanceSqr);
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    private static float distanceSqr(double x, double y, double z) {
        return (float) (x * x + y * y + z * z);
    }

    @Override
    public Deserializer<ParticleLifetimeKillPlane> deserializer() {
        return ParticleLifetimeKillPlane::fromJson;
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

    public static ParticleLifetimeKillPlane fromJson(JsonElement element) {
        JsonArray array = element.getAsJsonArray();
        if (array.size() != 4) {
            throw new JsonParseException("Size of array must be 4: " + array);
        }
        return new ParticleLifetimeKillPlane(
                array.get(0).getAsFloat(),
                array.get(1).getAsFloat(),
                array.get(2).getAsFloat(),
                array.get(3).getAsFloat()
        );
    }
}

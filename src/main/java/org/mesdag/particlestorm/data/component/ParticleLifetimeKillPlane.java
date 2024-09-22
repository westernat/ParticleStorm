package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.List;

/**
 * Particles that cross this plane expire.<p>
 * The plane is relative to the emitter, but oriented in world space.<p>
 * The four parameters are the usual 4 elements of a plane equation.<p>
 * A*x + B*y + C*z + D = 0 with the parameters being [ A, B, C, D ]
 */
public record ParticleLifetimeKillPlane(float A, float B, float C, float D) implements IParticleComponent {
    public static final Codec<ParticleLifetimeKillPlane> CODEC = Codec.list(Codec.FLOAT, 4, 4).xmap(
            floats -> new ParticleLifetimeKillPlane(floats.getFirst(), floats.get(1), floats.get(2), floats.get(3)),
            plane -> List.of(plane.A, plane.B, plane.C, plane.D)
    );

    @Override
    public Codec<ParticleLifetimeKillPlane> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of();
    }
}

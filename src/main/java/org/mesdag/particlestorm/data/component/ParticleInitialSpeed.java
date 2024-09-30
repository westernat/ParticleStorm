package org.mesdag.particlestorm.data.component;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import org.mesdag.particlestorm.data.molang.FloatMolangExp3;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

import java.util.List;

/**
 * Starts the particle with a specified speed, using the direction specified by the emitter shape.
 *
 * @param value Evaluated once
 * @param exp3  Evaluated once
 */
public record ParticleInitialSpeed(float value, FloatMolangExp3 exp3) implements IParticleComponent {
    public static final Codec<ParticleInitialSpeed> CODEC = Codec.either(Codec.FLOAT, FloatMolangExp3.CODEC).xmap(
            either -> either.map(f -> new ParticleInitialSpeed(f, FloatMolangExp3.ZERO), l -> new ParticleInitialSpeed(0, l)),
            speed -> speed.exp3 == FloatMolangExp3.ZERO ? Either.left(speed.value) : Either.right(speed.exp3)
    );

    @Override
    public Codec<ParticleInitialSpeed> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(exp3.exp1(), exp3.exp2(), exp3.exp3());
    }

    @Override
    public void apply(MolangParticleInstance instance) {
        if (exp3 == FloatMolangExp3.ZERO) {
            instance.setParticleSpeed(
                    instance.getXd() * value,
                    instance.getYd() * value,
                    instance.getZd() * value
            );
        } else {
            float[] mul = exp3.calculate(instance);
            instance.setParticleSpeed(
                    instance.getXd() * mul[0],
                    instance.getYd() * mul[1],
                    instance.getZd() * mul[2]
            );
        }
    }
}

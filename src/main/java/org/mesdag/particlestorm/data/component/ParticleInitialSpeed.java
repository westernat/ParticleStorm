package org.mesdag.particlestorm.data.component;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.FloatMolangExp3;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.util.List;

/**
 * Starts the particle with a specified speed, using the direction specified by the emitter shape.
 *
 * @param exp  Evaluated once
 * @param exp3 Evaluated once
 */
public record ParticleInitialSpeed(FloatMolangExp exp, FloatMolangExp3 exp3) implements IEmitterComponent {
    public static final Codec<ParticleInitialSpeed> CODEC = Codec.either(FloatMolangExp.CODEC, FloatMolangExp3.CODEC).xmap(
            either -> either.map(f -> new ParticleInitialSpeed(f, FloatMolangExp3.ZERO), l -> new ParticleInitialSpeed(FloatMolangExp.ZERO, l)),
            speed -> speed.exp3 == FloatMolangExp3.ZERO ? Either.left(speed.exp) : Either.right(speed.exp3)
    );

    @Override
    public Codec<ParticleInitialSpeed> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(exp, exp3.exp1(), exp3.exp2(), exp3.exp3());
    }

    @Override
    public void apply(ParticleEmitter entity) {
        if (exp3 == FloatMolangExp3.ZERO) {
            float value = exp.calculate(entity);
            entity.particleInitialSpeed.set(value);
        } else {
            float[] mul = exp3.calculate(entity);
            entity.particleInitialSpeed.set(mul);
        }
    }

    @Override
    public int order() {
        return 500;
    }

    @Override
    public String toString() {
        return "ParticleInitialSpeed{" +
                "exp=" + exp +
                ", exp3=" + exp3 +
                '}';
    }
}

package org.mesdag.particlestorm.data.component;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.FloatMolangExp3;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

import java.util.List;

/**
 * Starts the particle with a specified speed, using the direction specified by the emitter shape.
 *
 * @param speed Evaluated once
 */
public record ParticleInitialSpeed(Either<FloatMolangExp, FloatMolangExp3> speed) implements IParticleComponent {
    public static final Codec<ParticleInitialSpeed> CODEC = Codec.either(FloatMolangExp.CODEC, FloatMolangExp3.CODEC).xmap(
            either -> either.map(f -> new ParticleInitialSpeed(Either.left(f)), l -> new ParticleInitialSpeed(Either.right(l))),
            ParticleInitialSpeed::speed
    );

    @Override
    public Codec<ParticleInitialSpeed> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return speed.map(List::of, exp3 -> List.of(exp3.exp1(), exp3.exp2(), exp3.exp3()));
    }

    @Override
    public void apply(MolangParticleInstance instance) {
        speed.ifLeft(exp -> {
            float value = exp.calculate(instance);
            instance.initialSpeed.set(value);
        }).ifRight(exp3 -> {
            float[] mul = exp3.calculate(instance);
            instance.initialSpeed.set(mul);
        });
    }

    @Override
    public int order() {
        return -1;
    }

    @Override
    public String toString() {
        return "ParticleInitialSpeed{" +
                "speed=" + speed +
                '}';
    }
}

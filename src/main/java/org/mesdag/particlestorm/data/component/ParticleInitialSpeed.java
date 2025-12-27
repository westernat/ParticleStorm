package org.mesdag.particlestorm.data.component;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import org.mesdag.particlestorm.api.Deserializer;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.FloatMolangExp3;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.List;

/// Starts the particle with a specified speed, using the direction specified by the emitter shape.
///
/// @param speed Evaluated once
public record ParticleInitialSpeed(Either<FloatMolangExp, FloatMolangExp3> speed) implements IParticleComponent {
    @Override
    public Deserializer<ParticleInitialSpeed> deserializer() {
        return ParticleInitialSpeed::fromJson;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return speed.map(List::of, exp3 -> List.of(exp3.exp1(), exp3.exp2(), exp3.exp3()));
    }

    @Override
    public void apply(IMolangParticleInstance instance) {
        speed.ifLeft(exp -> {
            float value = exp.calculate(instance);
            instance.getInitialSpeed().set(value);
        }).ifRight(exp3 -> {
            float[] mul = exp3.calculate(instance);
            instance.getInitialSpeed().set(mul);
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

    public static ParticleInitialSpeed fromJson(JsonElement element) {
        if (element.isJsonArray()) {
            return new ParticleInitialSpeed(Either.right(FloatMolangExp3.fromJson(element)));
        }
        return new ParticleInitialSpeed(Either.left(FloatMolangExp.fromJson(element)));
    }
}

package org.mesdag.particlestorm.data.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.mesdag.particlestorm.api.Deserializer;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.FloatMolangExp3;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.List;

/// This component directly controls the particle.
///
/// @param relativePosition Directly set the position relative to the emitter. Defaults to `[0, 0, 0]`<p>
///                         E.g. a spiral might be:
///                         ```
///                         "relative_position": ["Math.cos(Params.LifeTime)", 1.0, "Math.sin(Params.Lifetime)"]
///                         ```
///                         Evaluated every frame
/// @param direction        Directly set the 3d direction of the particle<p>
///                         Doesn't affect direction if not specified<p>
///                         Evaluated every frame
/// @param rotation         Directly set the rotation of the particle<p>
///                         Evaluated every frame
public record ParticleMotionParametric(FloatMolangExp3 relativePosition, FloatMolangExp3 direction, FloatMolangExp rotation) implements IParticleComponent {
    @Override
    public Deserializer<ParticleMotionParametric> deserializer() {
        return ParticleMotionParametric::fromJson;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(
                relativePosition.exp1(), relativePosition.exp2(), relativePosition.exp3(),
                direction.exp1(), direction.exp2(), direction.exp3(), rotation
        );
    }

    @Override
    public void update(IMolangParticleInstance instance) {
        float[] pos = relativePosition.calculate(instance);
        instance.moveDirectly(pos[0], pos[1], pos[2]);
        if (direction != FloatMolangExp3.ZERO) {
            float[] dir = direction.calculate(instance);
            instance.self().setParticleSpeed(dir[0], dir[1], dir[2]);
        }
        instance.setZRot(rotation.calculate(instance));
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public String toString() {
        return "ParticleMotionParametric{" +
                "relativePosition=" + relativePosition +
                ", direction=" + direction +
                ", rotation=" + rotation +
                '}';
    }

    public static ParticleMotionParametric fromJson(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        return new ParticleMotionParametric(
                FloatMolangExp3.fromJson(object.get("relative_position")),
                FloatMolangExp3.fromJson(object.get("direction")),
                FloatMolangExp.fromJson(object.get("rotation"))
        );
    }
}

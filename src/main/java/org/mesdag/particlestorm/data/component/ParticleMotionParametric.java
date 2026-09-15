package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.FloatMolangExp3;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.ParticleEmitter;

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
    public static final Codec<ParticleMotionParametric> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FloatMolangExp3.CODEC.lenientOptionalFieldOf("relative_position", FloatMolangExp3.ZERO).forGetter(ParticleMotionParametric::relativePosition),
            FloatMolangExp3.CODEC.lenientOptionalFieldOf("direction", FloatMolangExp3.ZERO).forGetter(ParticleMotionParametric::direction),
            FloatMolangExp.CODEC.lenientOptionalFieldOf("rotation", FloatMolangExp.ZERO).forGetter(ParticleMotionParametric::rotation)
    ).apply(instance, ParticleMotionParametric::new));

    @Override
    public Codec<ParticleMotionParametric> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(
                relativePosition.exp1(), relativePosition.exp2(), relativePosition.exp3(),
                direction.exp1(), direction.exp2(), direction.exp3(), rotation
        );
    }

    private static final Vector3f vec = new Vector3f();

    @Override
    public void update(IMolangParticleInstance instance) {
        if (relativePosition != FloatMolangExp3.ZERO) {
            vec.set(relativePosition.calculate(instance));
            ParticleEmitter emitter = instance.getEmitter();
            if (emitter.isLocalSpace()) {
                if (!emitter.getPreset().localPosition) {
                    vec.mulDirection(emitter.getLocalSpace());
                    Vec3 emitterPos = emitter.getPosition();
                    vec.add((float) emitterPos.x, (float) emitterPos.y, (float) emitterPos.z);
                }
            } else {
                Vec3 emitterPos = emitter.getPosition();
                vec.add((float) emitterPos.x, (float) emitterPos.y, (float) emitterPos.z);
            }
            instance.self().setPos(vec.x, vec.y, vec.z);
        }
        if (direction != FloatMolangExp3.ZERO) {
            vec.set(direction.calculate(instance)).normalize();
            instance.self().setParticleSpeed(vec.x, vec.y, vec.z);
        }
        if (rotation != FloatMolangExp.ZERO) {
            instance.setZRot(rotation.calculate(instance));
        }
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
}

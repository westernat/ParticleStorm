package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.FloatMolangExp3;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

import java.util.List;

/**
 * This component specifies the dynamic properties of the particle, from a simulation standpoint what forces act upon the particle?<p>
 * These dynamics alter the velocity of the particle, which is a combination of the direction of the particle and the speed.<p>
 * Particle direction will always be in the direction of the velocity of the particle.
 *
 * @param linerAcceleration       The linear acceleration applied to the particle, defaults to [0, 0, 0].<p>
 *                                Units are blocks/sec/sec<p>
 *                                An example would be gravity which is [0, -9.8, 0]<p>
 *                                Evaluated every frame
 * @param linearDragCoefficient   Equation is acceleration = -linear_drag_coefficient*velocity<p>
 *                                Where velocity is the current direction times speed<p>
 *                                Think of this as air-drag. The higher the exp, the more drag<p>
 *                                Evaluated every frame
 * @param rotationAcceleration    Acceleration applies to the rotation speed of the particle<p>
 *                                Think of a disc spinning up or a smoke puff that starts rotating but slows down over time<p>
 *                                Evaluated every frame<p>
 *                                Acceleration is in degrees/sec/sec
 * @param rotationDragCoefficient Drag applied to slow down rotation<p>
 *                                Equation is rotation_acceleration += -rotation_rate*rotation_drag_coefficient<p>
 *                                Useful to slow a rotation, or to limit the rotation acceleration<p>
 *                                Think of a disc that speeds up (acceleration) but reaches a terminal speed (drag)<p>
 *                                Another use is if you have a particle growing in size, having the rotation slow down due to drag can add "weight" to the particle's motion
 */
public record ParticleMotionDynamic(FloatMolangExp3 linerAcceleration, FloatMolangExp linearDragCoefficient, FloatMolangExp rotationAcceleration,
                                    FloatMolangExp rotationDragCoefficient) implements IParticleComponent {
    public static final ResourceLocation ID = ResourceLocation.withDefaultNamespace("particle_motion_dynamic");
    public static final Codec<ParticleMotionDynamic> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FloatMolangExp3.CODEC.fieldOf("linear_acceleration").orElse(FloatMolangExp3.ZERO).forGetter(ParticleMotionDynamic::linerAcceleration),
            FloatMolangExp.CODEC.fieldOf("linear_drag_coefficient").orElse(FloatMolangExp.ZERO).forGetter(ParticleMotionDynamic::linearDragCoefficient),
            FloatMolangExp.CODEC.fieldOf("rotation_acceleration").orElse(FloatMolangExp.ZERO).forGetter(ParticleMotionDynamic::rotationAcceleration),
            FloatMolangExp.CODEC.fieldOf("rotation_drag_coefficient").orElse(FloatMolangExp.ZERO).forGetter(ParticleMotionDynamic::rotationDragCoefficient)
    ).apply(instance, ParticleMotionDynamic::new));

    @Override
    public Codec<ParticleMotionDynamic> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(
                linerAcceleration.exp1(), linerAcceleration.exp2(), linerAcceleration.exp3(),
                linearDragCoefficient, rotationAcceleration, rotationDragCoefficient
        );
    }

    @Override
    public void update(MolangParticleInstance instance) {
        apply(instance);
    }

    @Override
    public void apply(MolangParticleInstance instance) {
        float invTickRate = instance.emitter.invTickRate;

        float drag = -linearDragCoefficient.calculate(instance);
        instance.acceleration.set(linerAcceleration.calculate(instance));
        instance.acceleration.mul(invTickRate);
        instance.acceleration.add(
                instance.readOnlySpeed.x * drag,
                instance.readOnlySpeed.y * drag,
                instance.readOnlySpeed.z * drag
        );
        instance.addAcceleration();

        float c = -rotationDragCoefficient.calculate(instance);
        float u = rotationAcceleration.calculate(instance);
        u += c * instance.rolld;
        instance.rolld += u * invTickRate;
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public String toString() {
        return "ParticleMotionDynamic{" +
                "linerAcceleration=" + linerAcceleration +
                ", linearDragCoefficient=" + linearDragCoefficient +
                ", rotationAcceleration=" + rotationAcceleration +
                ", rotationDragCoefficient=" + rotationDragCoefficient +
                '}';
    }
}

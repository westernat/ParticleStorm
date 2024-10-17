package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

import java.util.List;

/**
 * Starts the particle with a specified orientation and rotation rate.
 *
 * @param rotation     Specifies the initial rotation in degrees. Evaluated once
 * @param rotationRate Specifies the spin rate in degrees/second. Evaluated once
 */
public record ParticleInitialSpin(FloatMolangExp rotation, FloatMolangExp rotationRate) implements IParticleComponent {
    public static final Codec<ParticleInitialSpin> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FloatMolangExp.CODEC.fieldOf("rotation").orElse(FloatMolangExp.ZERO).forGetter(ParticleInitialSpin::rotation),
            FloatMolangExp.CODEC.fieldOf("rotation_rate").orElse(FloatMolangExp.ZERO).forGetter(ParticleInitialSpin::rotationRate)
    ).apply(instance, ParticleInitialSpin::new));

    @Override
    public Codec<ParticleInitialSpin> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(rotation, rotationRate);
    }

    @Override
    public void apply(MolangParticleInstance instance) {
        instance.setRoll(rotation.calculate(instance) * Mth.DEG_TO_RAD);
        instance.rolld = rotationRate.calculate(instance) * instance.emitter.invTickRate * Mth.DEG_TO_RAD;
    }

    @Override
    public int order() {
        return 500;
    }
}

package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleGroup;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.network.EmitterManualPacketC2S;
import org.mesdag.particlestorm.particle.ParticleEmitterEntity;

import java.util.List;

public abstract class EmitterRate implements IEmitterComponent {
    public enum Type {
        INSTANT,
        STEADY,
        MANUAL
    }

    /**
     * All particles come out at once, then no more unless the emitter loops.
     */
    public static final class Instant extends EmitterRate {
        public static final Codec<Instant> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FloatMolangExp.CODEC.fieldOf("num_particles").orElseGet(() -> FloatMolangExp.ofConstant(10)).forGetter(Instant::getNumParticles)
        ).apply(instance, Instant::new));
        private final FloatMolangExp numParticles;

        public Instant(FloatMolangExp numParticles) {
            this.numParticles = numParticles;
        }

        public FloatMolangExp getNumParticles() {
            return numParticles;
        }

        @Override
        public Codec<Instant> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(numParticles);
        }

        @Override
        public void apply(ParticleEmitterEntity entity) {
            int calculate = (int) numParticles.calculate(entity);
            if (entity.spawnRate != calculate) {
                entity.spawnRate = calculate;
                entity.particleGroup = new ParticleGroup(calculate);
            }
        }

        @Override
        public String toString() {
            return "Instant{" +
                    "numParticles=" + numParticles +
                    '}';
        }
    }

    /**
     * Particles come out at a steady or Molang rate over time.
     */
    public static final class Steady extends EmitterRate {
        public static final Codec<Steady> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FloatMolangExp.CODEC.fieldOf("spawn_rate").orElse(FloatMolangExp.ONE).forGetter(Steady::getSpawnRate),
                FloatMolangExp.CODEC.fieldOf("max_particles").orElseGet(() -> FloatMolangExp.ofConstant(50)).forGetter(Steady::getMaxParticles)
        ).apply(instance, Steady::new));
        private final FloatMolangExp spawnRate;
        private final FloatMolangExp maxParticles;

        public Steady(FloatMolangExp spawnRate, FloatMolangExp maxParticles) {
            this.spawnRate = spawnRate;
            this.maxParticles = maxParticles;
        }

        public FloatMolangExp getSpawnRate() {
            return spawnRate;
        }

        public FloatMolangExp getMaxParticles() {
            return maxParticles;
        }

        @Override
        public Codec<Steady> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(spawnRate, maxParticles);
        }

        @Override
        public void apply(ParticleEmitterEntity entity) {
            int calculated = Math.max((int) (20.0F / spawnRate.calculate(entity)), 1);
            if (entity.spawnRate != calculated) {
                entity.spawnRate = calculated;
                entity.particleGroup = new ParticleGroup((int) maxParticles.calculate(entity));
            }
        }

        @Override
        public String toString() {
            return "Steady{" +
                    "spawnRate=" + spawnRate +
                    ", maxParticles=" + maxParticles +
                    '}';
        }
    }

    /**
     * Particle emission will occur only when the emitter is told to emit via the game itself. This is mostly used by legacy particle effects.
     */
    public static final class Manual extends EmitterRate {
        public static final Codec<Manual> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FloatMolangExp.CODEC.fieldOf("max_particles").orElse(FloatMolangExp.ZERO).forGetter(Manual::getMaxParticles)
        ).apply(instance, Manual::new));
        private final FloatMolangExp maxParticles;

        public Manual(FloatMolangExp maxParticles) {
            this.maxParticles = maxParticles;
        }

        public FloatMolangExp getMaxParticles() {
            return maxParticles;
        }

        @Override
        public Codec<Manual> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(maxParticles);
        }

        @Override
        public void update(ParticleEmitterEntity entity) {
            PacketDistributor.sendToServer(new EmitterManualPacketC2S(entity.getId(), (int) maxParticles.calculate(entity)));
        }

        @Override
        public boolean requireUpdate() {
            return true;
        }

        @Override
        public String toString() {
            return "Manual{" +
                    "maxParticles=" + maxParticles +
                    '}';
        }
    }
}

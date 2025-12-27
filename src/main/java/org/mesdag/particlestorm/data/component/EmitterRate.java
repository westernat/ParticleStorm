package org.mesdag.particlestorm.data.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.api.Deserializer;
import org.mesdag.particlestorm.api.IEmitterComponent;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.MutableParticleGroup;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.util.List;

public abstract sealed class EmitterRate implements IEmitterComponent permits EmitterRate.Instant, EmitterRate.Steady, EmitterRate.Manual {
    @Override
    public int order() {
        return 500;
    }

    public enum Type {
        INSTANT,
        STEADY,
        MANUAL
    }

    /// All particles come out at once, then no more unless the emitter loops.
    public static final class Instant extends EmitterRate {
        private final FloatMolangExp numParticles;

        public Instant(FloatMolangExp numParticles) {
            this.numParticles = numParticles;
        }

        public FloatMolangExp getNumParticles() {
            return numParticles;
        }

        @Override
        public Deserializer<Instant> deserializer() {
            return Instant::fromJson;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(numParticles);
        }

        @Override
        public void apply(ParticleEmitter emitter) {
            int limit = (int) numParticles.calculate(emitter);
            if (emitter.spawnRate != limit) {
                emitter.spawnRate = limit;
                if (emitter.particleGroup == null) {
                    emitter.particleGroup = new MutableParticleGroup(16384);
                }
            }
        }

        @Override
        public String toString() {
            return "Instant{" +
                    "numParticles=" + numParticles +
                    '}';
        }

        public static Instant fromJson(JsonElement element) {
            return new Instant(FloatMolangExp.fromJson(element.getAsJsonObject().get("num_particles"), FloatMolangExp.ofConstant(10)));
        }
    }

    /// Particles come out at a steady or Molang rate over time.
    public static final class Steady extends EmitterRate {
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
        public Deserializer<Steady> deserializer() {
            return Steady::fromJson;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(spawnRate, maxParticles);
        }

        @Override
        public void apply(ParticleEmitter emitter) {
            float calculated = spawnRate.calculate(emitter);
            float tickrate = PSGameClient.tickRate();
            emitter.spawnDuration = Math.max((int) (tickrate / calculated), 1);
            if (emitter.spawnRate != calculated) {
                emitter.spawnRate = emitter.spawnDuration == 1 ? (int) (calculated / tickrate) : 1;
                int limit = (int) maxParticles.calculate(emitter);
                if (emitter.particleGroup == null) {
                    emitter.particleGroup = new MutableParticleGroup(limit);
                } else {
                    emitter.particleGroup.setLimit(limit);
                }
            }
        }

        @Override
        public String toString() {
            return "Steady{" +
                    "spawnRate=" + spawnRate +
                    ", maxParticles=" + maxParticles +
                    '}';
        }

        public static Steady fromJson(JsonElement element) {
            JsonObject object = element.getAsJsonObject();
            FloatMolangExp exp = FloatMolangExp.fromJson(object.get("spawn_rate"), FloatMolangExp.ONE);
            FloatMolangExp exp1 = FloatMolangExp.fromJson(object.get("max_particles"), FloatMolangExp.ofConstant(50));
            return new Steady(exp, exp1);
        }
    }

    /// Particle emission will occur only when the emitter is told to emit via the game itself. This is mostly used by legacy particle effects.
    public static final class Manual extends EmitterRate {
        private final FloatMolangExp maxParticles;

        public Manual(FloatMolangExp maxParticles) {
            this.maxParticles = maxParticles;
        }

        public FloatMolangExp getMaxParticles() {
            return maxParticles;
        }

        @Override
        public Deserializer<Manual> deserializer() {
            return Manual::fromJson;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(maxParticles);
        }

        @Override
        public void apply(ParticleEmitter emitter) {
            int limit = (int) maxParticles.calculate(emitter);
            if (emitter.particleGroup == null) {
                emitter.particleGroup = new MutableParticleGroup(limit);
            } else {
                emitter.particleGroup.setLimit(limit);
            }
            emitter.spawnRate = limit;
        }

        @Override
        public String toString() {
            return "Manual{" +
                    "maxParticles=" + maxParticles +
                    '}';
        }

        public static Manual fromJson(JsonElement element) {
            return new Manual(FloatMolangExp.fromJson(element.getAsJsonObject().get("max_particles")));
        }
    }
}

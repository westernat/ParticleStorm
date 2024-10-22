package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.util.List;

public abstract class EmitterLifetime implements IEmitterComponent {
    /**
     * Emitter will turn 'on' when the activation expression is non-zero, and will turn 'off' when it's zero.
     * <p>
     * This is useful for situations like driving an entity-attached emitter from an entity variable.
     */
    public static class Expression extends EmitterLifetime {
        public static final Codec<Expression> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FloatMolangExp.CODEC.fieldOf("activation_expression").orElse(FloatMolangExp.ONE).forGetter(Expression::getActivationExpression),
                FloatMolangExp.CODEC.fieldOf("expiration_expression").orElse(FloatMolangExp.ZERO).forGetter(Expression::getExpirationExpression)
        ).apply(instance, Expression::new));
        private final FloatMolangExp activationExpression;
        private final FloatMolangExp expirationExpression;

        public Expression(FloatMolangExp activationExpression, FloatMolangExp expirationExpression) {
            this.activationExpression = activationExpression;
            this.expirationExpression = expirationExpression;
        }

        /**
         * When the expression is non-zero, the emitter will emit particles.
         * <p>
         * Evaluated every frame
         */
        public FloatMolangExp getActivationExpression() {
            return activationExpression;
        }

        /**
         * Emitter will expire if the expression is non-zero.
         * <p>
         * Evaluated every frame
         */
        public FloatMolangExp getExpirationExpression() {
            return expirationExpression;
        }

        @Override
        public Codec<Expression> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(activationExpression, expirationExpression);
        }

        @Override
        public void update(ParticleEmitter emitter) {
            if (expirationExpression.calculate(emitter) != 0.0) {
                emitter.remove();
            }
            emitter.active = activationExpression.calculate(emitter) != 0.0;
            emitter.lifetime = emitter.age;
        }

        @Override
        public boolean requireUpdate() {
            return true;
        }

        @Override
        public String toString() {
            return "Expression{" +
                    "activationExpression=" + activationExpression +
                    ", expirationExpression=" + expirationExpression +
                    '}';
        }
    }

    /**
     * Emitter will loop until it is removed.
     */
    public static class Looping extends EmitterLifetime {
        public static final Codec<Looping> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FloatMolangExp.CODEC.fieldOf("active_time").orElseGet(() -> FloatMolangExp.ofConstant(10)).forGetter(Looping::getActiveTime),
                FloatMolangExp.CODEC.fieldOf("sleep_time").orElse(FloatMolangExp.ZERO).forGetter(Looping::getSleepTime)
        ).apply(instance, Looping::new));
        private final FloatMolangExp activeTime;
        private final FloatMolangExp sleepTime;

        public Looping(FloatMolangExp activeTime, FloatMolangExp sleepTime) {
            this.activeTime = activeTime;
            this.sleepTime = sleepTime;
        }

        /**
         * Emitter will emit particles for this time per loop
         * <p>
         * Evaluated once per particle emitter loop
         */
        public FloatMolangExp getActiveTime() {
            return activeTime;
        }

        /**
         * Emitter will pause emitting particles for this time per loop
         * <p>
         * Evaluated once per particle emitter loop
         */
        public FloatMolangExp getSleepTime() {
            return sleepTime;
        }

        @Override
        public Codec<Looping> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(activeTime, sleepTime);
        }

        @Override
        public void update(ParticleEmitter emitter) {
            emitter.activeTime = (int) (activeTime.calculate(emitter) * 20);
            emitter.fullLoopTime = emitter.activeTime + (int) (sleepTime.calculate(emitter) * 20);
            emitter.lifetime = emitter.fullLoopTime;
            if (emitter.loopingTime < emitter.fullLoopTime) {
                emitter.active = emitter.loopingTime <= emitter.activeTime;
                emitter.loopingTime++;
            } else {
                emitter.spawned = false;
                emitter.loopingTime = 0;
                emitter.age = 1;
                for (IEmitterComponent e : emitter.getDetail().components) {
                    e.apply(emitter);
                }
                emitter.updateRandoms(emitter.level.random);
            }
        }

        @Override
        public boolean requireUpdate() {
            return true;
        }

        @Override
        public String toString() {
            return "Looping{" +
                    "activeTime=" + activeTime +
                    ", sleepTime=" + sleepTime +
                    '}';
        }
    }

    public static class Once extends EmitterLifetime {
        public static final Codec<Once> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FloatMolangExp.CODEC.fieldOf("active_time").orElseGet(() -> FloatMolangExp.ofConstant(10)).forGetter(Once::getActiveTime)
        ).apply(instance, Once::new));
        private final FloatMolangExp activeTime;

        public Once(FloatMolangExp activeTime) {
            this.activeTime = activeTime;
        }

        public FloatMolangExp getActiveTime() {
            return activeTime;
        }

        @Override
        public Codec<Once> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(activeTime);
        }

        @Override
        public void update(ParticleEmitter emitter) {
            if (emitter.age >= emitter.lifetime) {
                emitter.remove();
            }
        }

        @Override
        public void apply(ParticleEmitter emitter) {
            emitter.lifetime = (int) (activeTime.calculate(emitter) * 20);
        }

        @Override
        public boolean requireUpdate() {
            return true;
        }

        @Override
        public String toString() {
            return "Once{" +
                    "activeTime=" + activeTime +
                    '}';
        }
    }
}

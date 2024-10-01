package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.network.EmitterDiscardPacketC2S;
import org.mesdag.particlestorm.particle.ParticleEmitterEntity;

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
        public void update(ParticleEmitterEntity entity) {
            if (expirationExpression.calculate(entity) != 0.0) {
                PacketDistributor.sendToServer(new EmitterDiscardPacketC2S(entity.getId()));
            }
            entity.active = activationExpression.calculate(entity) != 0.0;
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
        public void update(ParticleEmitterEntity entity) {
            entity.activeTime = (int) (activeTime.calculate(entity) * 20);
            entity.fullLoopTime = entity.activeTime + (int) (sleepTime.calculate(entity) * 20);
            if (entity.loopingTime <= entity.fullLoopTime) {
                entity.active = entity.loopingTime <= entity.activeTime;
                entity.loopingTime++;
            } else {
                entity.spawned = false;
                entity.loopingTime = 0;
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
        public void update(ParticleEmitterEntity entity) {
            if (entity.age >= entity.lifetime) {
                PacketDistributor.sendToServer(new EmitterDiscardPacketC2S(entity.getId()));
            }
        }

        @Override
        public void apply(ParticleEmitterEntity entity) {
            entity.lifetime = (int) (activeTime.calculate(entity) * 20);
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

package org.mesdag.particlestorm.data.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.mesdag.particlestorm.api.Deserializer;
import org.mesdag.particlestorm.api.IEmitterComponent;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.util.List;

public abstract sealed class EmitterLifetime implements IEmitterComponent permits EmitterLifetime.Expression, EmitterLifetime.Looping, EmitterLifetime.Once {
    /// Emitter will turn 'on' when the activation expression is non-zero, and will turn 'off' when it's zero.
    ///
    /// This is useful for situations like driving an entity-attached emitter from an entity variable.
    public static final class Expression extends EmitterLifetime {
        private final FloatMolangExp activationExpression;
        private final FloatMolangExp expirationExpression;

        public Expression(FloatMolangExp activationExpression, FloatMolangExp expirationExpression) {
            this.activationExpression = activationExpression;
            this.expirationExpression = expirationExpression;
        }

        /// When the expression is non-zero, the emitter will emit particles.
        ///
        /// Evaluated every frame
        public FloatMolangExp getActivationExpression() {
            return activationExpression;
        }

        /// Emitter will expire if the expression is non-zero.
        ///
        /// Evaluated every frame
        public FloatMolangExp getExpirationExpression() {
            return expirationExpression;
        }

        @Override
        public Deserializer<Expression> deserializer() {
            return Expression::fromJson;
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

        public static Expression fromJson(JsonElement element) {
            JsonObject object = element.getAsJsonObject();
            FloatMolangExp exp = FloatMolangExp.fromJson(object.get("activation_expression"), FloatMolangExp.ONE);
            FloatMolangExp exp1 = FloatMolangExp.fromJson(object.get("expiration_expression"));
            return new Expression(exp, exp1);
        }
    }

    /// Emitter will loop until it is removed.
    public static final class Looping extends EmitterLifetime {
        private final FloatMolangExp activeTime;
        private final FloatMolangExp sleepTime;

        public Looping(FloatMolangExp activeTime, FloatMolangExp sleepTime) {
            this.activeTime = activeTime;
            this.sleepTime = sleepTime;
        }

        /// Emitter will emit particles for this time per loop
        ///
        /// Evaluated once per particle emitter loop
        public FloatMolangExp getActiveTime() {
            return activeTime;
        }

        /// Emitter will pause emitting particles for this time per loop
        ///
        /// Evaluated once per particle emitter loop
        public FloatMolangExp getSleepTime() {
            return sleepTime;
        }

        @Override
        public Deserializer<Looping> deserializer() {
            return Looping::fromJson;
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
                for (IEmitterComponent e : emitter.getPreset().components) {
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

        public static Looping fromJson(JsonElement element) {
            JsonObject object = element.getAsJsonObject();
            FloatMolangExp exp = FloatMolangExp.fromJson(object.get("active_time"), FloatMolangExp.ofConstant(10));
            FloatMolangExp exp1 = FloatMolangExp.fromJson(object.get("sleep_time"));
            return new Looping(exp, exp1);
        }
    }

    public static final class Once extends EmitterLifetime {
        private final FloatMolangExp activeTime;

        public Once(FloatMolangExp activeTime) {
            this.activeTime = activeTime;
        }

        public FloatMolangExp getActiveTime() {
            return activeTime;
        }

        @Override
        public Deserializer<Once> deserializer() {
            return Once::fromJson;
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

        public static Once fromJson(JsonElement element) {
            return new Once(FloatMolangExp.fromJson(element.getAsJsonObject().get("active_time"), FloatMolangExp.ofConstant(10)));
        }
    }
}

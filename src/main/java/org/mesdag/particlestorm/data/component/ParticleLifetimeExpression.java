package org.mesdag.particlestorm.data.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.mesdag.particlestorm.api.Deserializer;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.List;

/// Standard lifetime component. These expressions control the lifetime of the particle.
///
/// @param expirationExpression This expression makes the particle expire when true (non-zero)<p>
///                                                         The float/expr is evaluated once per particle<p>
///                                                         Evaluated every frame<p>
/// @param maxLifetime          Alternate way to express lifetime<p>
///                                                         Particle will expire after this much time<p>
///                                                         Evaluated once<p>
///                                                         Available value is `[0.05, infinite)`
public record ParticleLifetimeExpression(FloatMolangExp expirationExpression, FloatMolangExp maxLifetime) implements IParticleComponent {
    @Override
    public Deserializer<ParticleLifetimeExpression> deserializer() {
        return ParticleLifetimeExpression::fromJson;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(expirationExpression, maxLifetime);
    }

    @Override
    public void update(IMolangParticleInstance instance) {
        if (expirationExpression.initialized() && expirationExpression.getVariable().get(instance) != 0.0) {
            instance.self().remove();
        }
    }

    @Override
    public void apply(IMolangParticleInstance instance) {
        if (maxLifetime.initialized()) {
            instance.self().setLifetime(Math.max((int) (maxLifetime.calculate(instance) * 20), 1));
        }
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public String toString() {
        return "ParticleLifetimeExpression{" +
                "expirationExpression=" + expirationExpression +
                ", maxLifetime=" + maxLifetime +
                '}';
    }

    public static ParticleLifetimeExpression fromJson(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        FloatMolangExp exp = FloatMolangExp.fromJson(object.get("expiration_expression"));
        FloatMolangExp exp1 = FloatMolangExp.fromJson(object.get("max_lifetime"));
        return new ParticleLifetimeExpression(exp, exp1);
    }
}

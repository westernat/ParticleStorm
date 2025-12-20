package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.List;

/// Standard lifetime component. These expressions control the lifetime of the particle.
///
/// @param expirationExpression This expression makes the particle expire when true (non-zero)<p>
///                             The float/expr is evaluated once per particle<p>
///                             Evaluated every frame<p>
/// @param maxLifetime          Alternate way to express lifetime<p>
///                             Particle will expire after this much time<p>
///                             Evaluated once<p>
///                             Available value is `[0.05, infinite)`
public record ParticleLifetimeExpression(FloatMolangExp expirationExpression, FloatMolangExp maxLifetime) implements IParticleComponent {
    public static final Codec<ParticleLifetimeExpression> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FloatMolangExp.CODEC.fieldOf("expiration_expression").orElse(FloatMolangExp.ZERO).forGetter(ParticleLifetimeExpression::expirationExpression),
            FloatMolangExp.CODEC.fieldOf("max_lifetime").orElse(FloatMolangExp.ZERO).forGetter(ParticleLifetimeExpression::maxLifetime)
    ).apply(instance, ParticleLifetimeExpression::new));

    @Override
    public Codec<ParticleLifetimeExpression> codec() {
        return CODEC;
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
}

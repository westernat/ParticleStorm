package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.List;

/// Starts the particle with a specified render expression.
public record ParticleInitialization(FloatMolangExp perRenderExpression, FloatMolangExp perUpdateExpression) implements IParticleComponent {
    public static final ResourceLocation ID = ResourceLocation.withDefaultNamespace("particle_initialization");
    public static final Codec<ParticleInitialization> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FloatMolangExp.CODEC.lenientOptionalFieldOf("per_render_expression", FloatMolangExp.ZERO).forGetter(ParticleInitialization::perRenderExpression),
            FloatMolangExp.CODEC.lenientOptionalFieldOf("per_update_expression", FloatMolangExp.ZERO).forGetter(ParticleInitialization::perRenderExpression)
    ).apply(instance, ParticleInitialization::new));

    @Override
    public Codec<ParticleInitialization> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(perRenderExpression, perUpdateExpression);
    }

    @Override
    public void update(IMolangParticleInstance instance) {
        perRenderExpression.calculate(instance);
    }

    @Override
    public void apply(IMolangParticleInstance instance) {
        perRenderExpression.calculate(instance);
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public int order() {
        return 400;
    }

    @Override
    public String toString() {
        return "ParticleInitialization{" +
                "perRenderExpression=" + perRenderExpression +
                '}';
    }
}

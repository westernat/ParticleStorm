package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

import java.util.List;

/**
 * Starts the particle with a specified render expression.
 */
public record ParticleInitialization(FloatMolangExp perRenderExpression) implements IParticleComponent {
    public static final ResourceLocation ID = ResourceLocation.withDefaultNamespace("particle_initialization");
    public static final Codec<ParticleInitialization> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FloatMolangExp.CODEC.fieldOf("per_render_expression").forGetter(ParticleInitialization::perRenderExpression)
    ).apply(instance, ParticleInitialization::new));

    @Override
    public Codec<ParticleInitialization> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(perRenderExpression);
    }

    @Override
    public void update(MolangParticleInstance instance) {
        perRenderExpression.calculate(instance);
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public String toString() {
        return "ParticleInitialization{" +
                "perRenderExpression=" + perRenderExpression +
                '}';
    }
}

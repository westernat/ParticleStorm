package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.ParticleEmitterEntity;

import java.util.List;

/**
 * This component allows the emitter to run some Molang at creation, primarily to populate any Molang variables that get used later.
 *
 * @param creationExpression This is run once at emitter startup
 * @param perUpdateExpression This is run once per emitter update
 */
public record EmitterInitialization(MolangExp creationExpression, MolangExp perUpdateExpression) implements IEmitterComponent {
    public static final ResourceLocation ID = ResourceLocation.withDefaultNamespace("emitter_initialization");
    public static final Codec<EmitterInitialization> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MolangExp.CODEC.fieldOf("creation_expression").orElse(MolangExp.EMPTY).forGetter(EmitterInitialization::creationExpression),
            MolangExp.CODEC.fieldOf("per_update_expression").orElse(MolangExp.EMPTY).forGetter(EmitterInitialization::perUpdateExpression)
    ).apply(instance, EmitterInitialization::new));

    @Override
    public Codec<? extends IComponent> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(creationExpression, perUpdateExpression);
    }

    @Override
    public void update(ParticleEmitterEntity entity) {
        perUpdateExpression.calculate(entity);
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public String toString() {
        return "EmitterInitialization{" +
                "creationExpression=" + creationExpression +
                ", perUpdateExpression=" + perUpdateExpression +
                '}';
    }
}

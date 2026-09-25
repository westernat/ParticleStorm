package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import org.mesdag.particlestorm.api.IEmitterComponent;
import org.mesdag.particlestorm.data.ConditionDispatch;
import org.mesdag.particlestorm.data.molang.BoolMolangExp;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.util.List;

public record EmitterMultiShape(List<ConditionDispatch<EmitterShape>> shapes) implements IEmitterComponent {
    public static final Codec<EmitterMultiShape> CODEC = ConditionDispatch.codec(EmitterShape.MAP).listOf().xmap(EmitterMultiShape::new, EmitterMultiShape::shapes);

    @Override
    public Codec<EmitterMultiShape> codec() {
        return CODEC;
    }

    @Override
    public List<BoolMolangExp> getAllMolangExp() {
        return shapes.stream().map(ConditionDispatch::condition).toList();
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public void update(ParticleEmitter emitter) {
        for (ConditionDispatch<EmitterShape> shape : shapes) {
            if (shape.condition().get(emitter)) {
                shape.value().update(emitter);
            }
        }
    }
}

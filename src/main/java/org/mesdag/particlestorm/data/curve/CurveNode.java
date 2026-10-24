package org.mesdag.particlestorm.data.curve;


import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.mesdag.particlestorm.ParticleStorm;

public record CurveNode(float value, float slope) {
    public static final Codec<CurveNode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ParticleStorm.lenientOptionalFieldOf(Codec.FLOAT, "value", () -> 1.0F).forGetter(CurveNode::value),
            ParticleStorm.lenientOptionalFieldOf(Codec.FLOAT, "slope", () -> 1.0F).forGetter(CurveNode::slope)
    ).apply(instance, CurveNode::new));
}

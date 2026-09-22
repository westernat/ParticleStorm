package org.mesdag.particlestorm.data.curve;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CurveNode(float value, float slope) {
    public static final Codec<CurveNode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("value").orElse(1.0F).forGetter(CurveNode::value),
            Codec.FLOAT.fieldOf("slope").orElse(1.0F).forGetter(CurveNode::slope)
    ).apply(instance, CurveNode::new));
}

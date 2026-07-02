package org.mesdag.particlestorm.data.curve;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CurveNode(float value, float slope) {
    public static final Codec<CurveNode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.lenientOptionalFieldOf("value", 1.0F).forGetter(CurveNode::value),
            Codec.FLOAT.lenientOptionalFieldOf("slope", 1.0F).forGetter(CurveNode::slope)
    ).apply(instance, CurveNode::new));
}

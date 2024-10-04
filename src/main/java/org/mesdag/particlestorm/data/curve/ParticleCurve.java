package org.mesdag.particlestorm.data.curve;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;

public class ParticleCurve {
    public static final Codec<ParticleCurve> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CurveType.CODEC.fieldOf("type").orElse(CurveType.LINEAR).forGetter(curve -> curve.type),
            FloatMolangExp.CODEC.fieldOf("input").forGetter(curve -> curve.input),
            FloatMolangExp.CODEC.fieldOf("horizontal_range").orElse(FloatMolangExp.ONE).forGetter(curve -> curve.horizontalRange),
            CurveType.CODEC.dispatchMap(
                    nodes -> nodes.isLeft ? CurveType.BEZIER_CHAIN : CurveType.LINEAR,
                    curveType -> curveType == CurveType.BEZIER_CHAIN ? CurveNodes.MAP_CODEC.fieldOf("nodes") : CurveNodes.LIST_CODEC.fieldOf("nodes")
            ).forGetter(curve -> curve.nodes)
    ).apply(instance, ParticleCurve::new));
    public final CurveType type;
    public final FloatMolangExp input;
    public final FloatMolangExp horizontalRange;
    public final CurveNodes nodes;

    public ParticleCurve(CurveType type, FloatMolangExp input, FloatMolangExp horizontalRange, CurveNodes nodes) {
        this.type = type;
        this.input = input;
        this.horizontalRange = horizontalRange;
        this.nodes = nodes;
    }

    @Override
    public String toString() {
        return "ParticleCurve[" +
                "type=" + type + ", " +
                "input=" + input + ", " +
                "horizontalRange=" + horizontalRange + ", " +
                "nodes=" + nodes + ']';
    }
}

package org.mesdag.particlestorm.data.curve;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;

public record ParticleCurve(CurveType type, FloatMolangExp input, FloatMolangExp horizontalRange, CurveNodes nodes) {
    public static final Codec<ParticleCurve> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CurveType.CODEC.fieldOf("type").orElse(CurveType.LINEAR).forGetter(ParticleCurve::type),
            FloatMolangExp.CODEC.fieldOf("input").forGetter(ParticleCurve::input),
            FloatMolangExp.CODEC.fieldOf("horizontal_range").orElse(FloatMolangExp.ONE).forGetter(ParticleCurve::horizontalRange),
            CurveType.CODEC.dispatchMap(
                    nodes -> nodes.isLeft() ? CurveType.BEZIER_CHAIN : CurveType.LINEAR,
                    curveType -> curveType == CurveType.BEZIER_CHAIN ? CurveNodes.MAP_CODEC.fieldOf("nodes") : CurveNodes.LIST_CODEC.fieldOf("nodes")
            ).forGetter(ParticleCurve::nodes)
    ).apply(instance, ParticleCurve::new));
}

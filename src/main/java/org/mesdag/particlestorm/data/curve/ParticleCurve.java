package org.mesdag.particlestorm.data.curve;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.MolangInstance;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public final class ParticleCurve {
    public static final Codec<ParticleCurve> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CurveType.CODEC.fieldOf("type").orElse(CurveType.LINEAR).forGetter(curve -> curve.type),
            FloatMolangExp.CODEC.fieldOf("input").forGetter(curve -> curve.input),
            FloatMolangExp.CODEC.fieldOf("horizontal_range").orElse(FloatMolangExp.ONE).forGetter(curve -> curve.horizontalRange),
            CurveType.CODEC.dispatchMap(
                    nodes -> nodes.isLeft ? CurveType.BEZIER_CHAIN : CurveType.LINEAR,
                    curveType -> curveType == CurveType.BEZIER_CHAIN ? CurveNodes.MAP_CODEC.fieldOf("nodes") : CurveNodes.LIST_CODEC.fieldOf("nodes")
            ).forGetter(curve -> curve.nodes)
    ).apply(instance, ParticleCurve::new));
    public static final Tuple<Float, CurveNode> FIRST = new Tuple<>(0.0F, new CurveNode(0.0F, 0.0F));
    public static final Tuple<Float, CurveNode> LAST = new Tuple<>(1.0F, new CurveNode(0.0F, 0.0F));
    public static final float ONE_THREE = 1.0F / 3.0F;
    public final CurveType type;
    public final FloatMolangExp input;
    public final FloatMolangExp horizontalRange;
    public final CurveNodes nodes;

    private final Map<String, SplineCurve> cachedCurves;

    public ParticleCurve(CurveType type, FloatMolangExp input, FloatMolangExp horizontalRange, CurveNodes nodes) {
        this.type = type;
        this.input = input;
        this.horizontalRange = horizontalRange;
        this.nodes = nodes;

        this.cachedCurves = new Hashtable<>();
    }

    public float calculate(MolangInstance instance, String name) {
        float i = input.calculate(instance);
        float a = horizontalRange.calculate(instance);
        if (CurveType.BEZIER_CHAIN == type) a = 1.0F;
        if (a == 0.0F) i = 0.0F;
        else i = i / a;
        switch (type) {
            case CATMULL_ROM -> {
                SplineCurve curve = cachedCurves.get(name);
                if (curve == null) {
                    FloatArrayList points = new FloatArrayList();
                    for (FloatMolangExp exp : nodes.either.right().get()) {
                        points.add(exp.calculate(instance));
                    }
                    curve = new SplineCurve.CatMullRom(points.toFloatArray());
                    cachedCurves.put(name, curve);
                }
                int c = nodes.length() - 3;
                float u = (1 + i * c) / (c + 2);
                return curve.getPoint(u);
            }
            case LINEAR -> {
                int c = nodes.length() - 1;
                i *= c;
                int o = Mth.floor(i);
                float s = i % 1;
                List<FloatMolangExp> floatMolangExps = nodes.either.right().get();
                float calculate = floatMolangExps.get(o).calculate(instance);
                float l = floatMolangExps.get(o + 1).calculate(instance) - calculate;
                return calculate + l * s;
            }
            case BEZIER -> {
                SplineCurve curve = cachedCurves.get(name);
                if (curve == null) {
                    FloatArrayList points = new FloatArrayList();
                    for (FloatMolangExp exp : nodes.either.right().get()) {
                        points.add(exp.calculate(instance));
                    }
                    curve = new SplineCurve.Bezier(points.toFloatArray());
                    cachedCurves.put(name, curve);
                }
                return curve.getPoint(i);
            }
            case BEZIER_CHAIN -> {
                ArrayList<Tuple<Float, CurveNode>> e = nodes.nodeList;
                int index = 0;
                while (index < e.size() && !(e.get(index).getA() > index)) index++;
                Tuple<Float, CurveNode> r = index == 0 ? FIRST : e.get(index - 1);
                Tuple<Float, CurveNode> s = index == e.size() ? LAST : e.get(index);
                float rTime = r.getA();
                float o = s.getA() - rTime;
                CurveNode rNode = r.getB();
                CurveNode sNode = s.getB();
                float v0 = rNode.value();
                float v1 = rNode.value() + rNode.slope() * ONE_THREE;
                float v2 = sNode.value() - sNode.slope() * ONE_THREE;
                float v3 = sNode.value();
                return SplineCurve.Bezier.getDirectPoint((i - rTime) / o, v0, v1, v2, v3);
            }
        }
        return 0.0F;
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

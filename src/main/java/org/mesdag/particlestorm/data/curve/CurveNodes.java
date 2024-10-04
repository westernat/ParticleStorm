package org.mesdag.particlestorm.data.curve;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.util.Tuple;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CurveNodes {
    public static final Codec<CurveNodes> MAP_CODEC = Codec.unboundedMap(Codec.STRING, CurveNode.CODEC).xmap(
            map -> new CurveNodes(Either.left(map)),
            nodes -> nodes.either.left().get()
    );
    public static final Codec<CurveNodes> LIST_CODEC = Codec.list(FloatMolangExp.CODEC).xmap(
            list -> new CurveNodes(Either.right(list)),
            nodes -> nodes.either.right().get()
    );

    public final Either<Map<String, CurveNode>, List<FloatMolangExp>> either;
    public final boolean isLeft;

    public final LinkedList<Tuple<Float, CurveNode>> nodeList;

    public CurveNodes(Either<Map<String, CurveNode>, List<FloatMolangExp>> either) {
        this.either = either;
        this.isLeft = either.left().isPresent();

        this.nodeList = new LinkedList<>();
        if (isLeft) {
            either.left().get().entrySet().stream()
                    .map(entry -> new Tuple<>(Float.parseFloat(entry.getKey()), entry.getValue()))
                    .sorted(Comparator.comparing(Tuple::getA))
                    .forEachOrdered(nodeList::add);
        }
    }
}

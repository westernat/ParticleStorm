package org.mesdag.particlestorm.data.curve;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.datafixers.util.Pair;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class CurveNodes {
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

    public final ArrayList<Pair<Float, CurveNode>> nodeList;

    public CurveNodes(Either<Map<String, CurveNode>, List<FloatMolangExp>> either) {
        this.either = either;
        this.isLeft = either.left().isPresent();

        this.nodeList = new ArrayList<>();
        if (isLeft) {
            either.left().get().entrySet().stream()
                    .map(entry -> Pair.of(Float.parseFloat(entry.getKey()), entry.getValue()))
                    .sorted(Comparator.comparing(Pair::getFirst))
                    .forEachOrdered(nodeList::add);
        }
    }

    public int length() {
        if (isLeft) return either.left().get().size();
        return either.right().get().size();
    }
}

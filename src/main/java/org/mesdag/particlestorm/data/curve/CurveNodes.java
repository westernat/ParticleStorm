package org.mesdag.particlestorm.data.curve;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;

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

    private final Either<Map<String, CurveNode>, List<FloatMolangExp>> either;
    private final boolean left;

    public CurveNodes(Either<Map<String, CurveNode>, List<FloatMolangExp>> either) {
        this.either = either;
        this.left = either.left().isPresent();
    }

    public Either<Map<String, CurveNode>, List<FloatMolangExp>> getEither() {
        return either;
    }

    public boolean isLeft() {
        return left;
    }
}

package org.mesdag.particlestorm.data.molang;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FloatMolangExpList {
    public static final FloatMolangExpList EMPTY = new FloatMolangExpList(0, List.of());
    public static final Codec<FloatMolangExpList> CODEC = Codec.either(FloatMolangExp.CODEC, Codec.list(FloatMolangExp.CODEC)).xmap(
            either -> either.map(e -> new FloatMolangExpList(1, Collections.singletonList(e)), l -> new FloatMolangExpList(l.size(), l)),
            list -> list.size() == 1 ? Either.left(list.expressions.getFirst()) : Either.right(list.expressions)
    );
    private final int size;
    private final List<FloatMolangExp> expressions;

    public FloatMolangExpList(int size, List<FloatMolangExp> expressions) {
        if (size != expressions.size()) {
            throw new IllegalArgumentException("Size of " + size + " not match the size of expressions");
        }
        this.size = size;
        this.expressions = expressions;
    }

    public FloatMolangExpList(int size, FloatMolangExp... expressions) {
        this(size, Arrays.stream(expressions).toList());
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public List<FloatMolangExp> getExpressions() {
        return expressions;
    }

    public boolean isSingleExp() {
        return size == 1;
    }

    public FloatMolangExp getExp(int index) {
        Objects.checkIndex(index, size);
        return expressions.get(index);
    }
}

package org.mesdag.particlestorm.data.molang;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public record FloatMolangExpList(int size, List<FloatMolangExp> expressions) {
    public static final FloatMolangExpList EMPTY = new FloatMolangExpList(0, List.of());

    public FloatMolangExpList {
        if (size != expressions.size()) {
            throw new IllegalArgumentException("Size of " + size + " not match the size of expressions");
        }
    }

    public FloatMolangExpList(int size, FloatMolangExp... expressions) {
        this(size, Arrays.stream(expressions).toList());
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isSingleExp() {
        return size == 1;
    }

    public FloatMolangExp getExp(int index) {
        Objects.checkIndex(index, size);
        return expressions.get(index);
    }

    public static FloatMolangExpList fromJson(JsonElement element) {
        if (element.isJsonArray()) {
            ImmutableList.Builder<FloatMolangExp> builder = ImmutableList.builder();
            for (JsonElement jsonElement : element.getAsJsonArray()) {
                builder.add(FloatMolangExp.fromJson(jsonElement));
            }
            ImmutableList<FloatMolangExp> list = builder.build();
            return new FloatMolangExpList(list.size(), list);
        }
        return new FloatMolangExpList(1, FloatMolangExp.fromJson(element));
    }
}

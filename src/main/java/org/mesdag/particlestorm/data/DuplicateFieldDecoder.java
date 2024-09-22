package org.mesdag.particlestorm.data;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.FieldEncoder;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class DuplicateFieldDecoder<A> extends MapDecoder.Implementation<A> {
    protected final Set<String> names;
    private final Decoder<A> elementCodec;

    public DuplicateFieldDecoder(final Set<String> names, final Decoder<A> elementCodec) {
        this.names = names;
        this.elementCodec = elementCodec;
    }

    public static <T> MapCodec<T> fieldOf(String defaultName, Set<String> names, Codec<T> codec) {
        if (!names.contains(defaultName)) {
            throw new IllegalArgumentException("Argument 'names' must contains 'defaultName'!");
        }
        return MapCodec.of(new FieldEncoder<>(defaultName, codec), new DuplicateFieldDecoder<>(names, codec));
    }

    public static <T> MapCodec<T> fieldOf(String defaultName, String another, Codec<T> codec) {
        return MapCodec.of(new FieldEncoder<>(defaultName, codec), new DuplicateFieldDecoder<>(Set.of(defaultName, another), codec));
    }

    @Override
    public <T> DataResult<A> decode(final DynamicOps<T> ops, final MapLike<T> input) {
        for (String name : names) {
            final T value = input.get(name);
            if (value != null) {
                return elementCodec.parse(ops, value);
            }
        }
        return DataResult.error(() -> "No key " + names + " in " + input);
    }

    @Override
    public <T> Stream<T> keys(final DynamicOps<T> ops) {
        return names.stream().map(ops::createString);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DuplicateFieldDecoder<?> that = (DuplicateFieldDecoder<?>) o;
        return Objects.equals(names, that.names) && Objects.equals(elementCodec, that.elementCodec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(names, elementCodec);
    }

    @Override
    public String toString() {
        return "DuplicateFieldDecoder[" + names + ": " + elementCodec + ']';
    }
}

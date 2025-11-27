package org.mesdag.particlestorm.data;

import com.mojang.serialization.*;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class DuplicateFieldDecoder {
    @Deprecated
    public static <T> MapCodec<T> fieldOf(String defaultName, Set<String> names, Codec<T> codec) {
        return fieldOf(codec, defaultName, names.toArray(new String[0]));
    }

    @Deprecated
    public static <T> MapCodec<T> fieldOf(String defaultName, String another, Codec<T> codec) {
        return fieldOf(codec, defaultName, another);
    }

    public static <T> MapCodec<T> fieldOf(Codec<T> codec, String defaultName, String... alias) {
        String[] names = new String[alias.length + 1];
        names[0] = defaultName;
        System.arraycopy(alias, 0, names, 1, alias.length);
        return NeoForgeExtraCodecs.aliasedFieldOf(codec, names);
    }

    public static <T> MapCodec<Optional<T>> optionalFieldOf(Codec<T> codec, String defaultName, String... alias) {
        String[] names = new String[alias.length + 1];
        names[0] = defaultName;
        System.arraycopy(alias, 0, names, 1, alias.length);
        return new AliasOptionalFieldCodec<>(codec, names);
    }

    public static class AliasOptionalFieldCodec<A> extends MapCodec<Optional<A>> {
        private final Codec<A> elementCodec;
        private final String[] names;

        public AliasOptionalFieldCodec(Codec<A> elementCodec, String[] names) {
            this.elementCodec = elementCodec;
            this.names = Arrays.stream(names).distinct().sorted().toArray(String[]::new);
        }

        @Override
        public <T> DataResult<Optional<A>> decode(DynamicOps<T> ops, MapLike<T> input) {
            for (String name : names) {
                T t = input.get(name);
                if (t == null) continue;
                DataResult<A> parsed = elementCodec.parse(ops, t);
                if (parsed.isSuccess()) {
                    return parsed.map(Optional::of).setPartial(parsed.resultOrPartial());
                }
            }
            return DataResult.success(Optional.empty());
        }

        @Override
        public <T> RecordBuilder<T> encode(Optional<A> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            if (input.isEmpty()) return prefix;
            return prefix.add(names[0], elementCodec.encodeStart(ops, input.get()));
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Arrays.stream(names).map(ops::createString);
        }

        @Override
        public boolean equals(Object o) {
            return o == this || (
                    o instanceof AliasOptionalFieldCodec<?> that &&
                            Arrays.equals(names, that.names) &&
                            elementCodec.equals(that.elementCodec)
            );
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(names);
            result = 31 * result + Objects.hashCode(elementCodec);
            return result;
        }

        @Override
        public String toString() {
            return "AliasOptionalFieldCodec{" +
                    "names='" + Arrays.toString(names) + '\'' +
                    ", elementCodec=" + elementCodec +
                    '}';
        }
    }
}

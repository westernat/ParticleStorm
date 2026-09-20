package org.mesdag.particlestorm.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.RecordBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ParticleCodecs {
    private ParticleCodecs() {}

    public static <E> Codec<List<E>> list(Codec<E> element, int min, int max) {
        Function<List<E>, DataResult<List<E>>> validate = values -> values.size() >= min && values.size() <= max
                ? DataResult.success(values)
                : DataResult.error(() -> "Expected " + min + ".." + max + " elements, got " + values.size());
        return element.listOf().flatXmap(validate, validate);
    }

    public static <K, V> Codec<Map<K, V>> keyedMap(Codec<K> keyCodec, Function<K, Codec<V>> valueCodecs) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<Map<K, V>, T>> decode(DynamicOps<T> ops, T input) {
                return ops.getMap(input).flatMap(map -> {
                    DataResult<Map<K, V>> result = DataResult.success(new LinkedHashMap<>());
                    for (Pair<T, T> entry : map.entries().toList()) {
                        result = result.flatMap(values -> keyCodec.parse(ops, entry.getFirst()).flatMap(key -> {
                            Codec<V> valueCodec = valueCodecs.apply(key);
                            if (valueCodec == null) return DataResult.error(() -> "Unknown particle component: " + key);
                            return valueCodec.parse(ops, entry.getSecond()).map(value -> {
                                values.put(key, value);
                                return values;
                            });
                        }));
                    }
                    return result.map(values -> Pair.of(values, ops.empty()));
                });
            }

            @Override
            public <T> DataResult<T> encode(Map<K, V> input, DynamicOps<T> ops, T prefix) {
                RecordBuilder<T> builder = ops.mapBuilder();
                for (Map.Entry<K, V> entry : input.entrySet()) {
                    Codec<V> valueCodec = valueCodecs.apply(entry.getKey());
                    if (valueCodec == null) return DataResult.error(() -> "Unknown particle component: " + entry.getKey());
                    builder.add(keyCodec.encodeStart(ops, entry.getKey()), valueCodec.encodeStart(ops, entry.getValue()));
                }
                return builder.build(prefix);
            }
        };
    }
}

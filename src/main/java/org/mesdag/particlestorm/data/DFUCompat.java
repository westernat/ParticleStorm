package org.mesdag.particlestorm.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.RecordBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class DFUCompat {
    /// Replacement for Codec.dispatchedMap in DFU 6.0.8
    public static <K, V> Codec<Map<K, V>> dispatchedMap(Codec<K> keyCodec, Function<K, Codec<V>> dispatch) {
        return new Codec<>() {
            @Override
            public <T> DataResult<T> encode(Map<K, V> input, DynamicOps<T> ops, T prefix) {
                RecordBuilder<T> builder = ops.mapBuilder();
                for (Map.Entry<K, V> entry : input.entrySet()) {
                    DataResult<T> encodedKey = keyCodec.encodeStart(ops, entry.getKey());
                    if (encodedKey.result().isPresent()) {
                        Codec<V> valueCodec = dispatch.apply(entry.getKey());
                        DataResult<T> encodedValue = valueCodec.encodeStart(ops, entry.getValue());
                        if (encodedValue.result().isPresent()) {
                            builder.add(encodedKey.result().get(), encodedValue.result().get());
                        }
                    }
                }
                return builder.build(prefix);
            }

            @Override
            public <T> DataResult<Pair<Map<K, V>, T>> decode(DynamicOps<T> ops, T input) {
                return ops.getMap(input).flatMap(mapLike -> {
                    Map<K, V> result = new LinkedHashMap<>();
                    mapLike.entries().forEach(entry -> {
                        DataResult<K> keyResult = keyCodec.parse(ops, entry.getFirst());
                        if (keyResult.result().isPresent()) {
                            K key = keyResult.result().get();
                            Codec<V> valueCodec = dispatch.apply(key);
                            DataResult<V> valueResult = valueCodec.parse(ops, entry.getSecond());
                            if (valueResult.result().isPresent()) {
                                result.put(key, valueResult.result().get());
                            }
                        }
                    });
                    return DataResult.success(Pair.of(result, input));
                });
            }
        };
    }
}

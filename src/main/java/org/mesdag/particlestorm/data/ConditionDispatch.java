package org.mesdag.particlestorm.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.data.molang.BoolMolangExp;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public record ConditionDispatch<T>(BoolMolangExp condition, T value) {
    public static <T> Codec<ConditionDispatch<T>> codec(Map<ResourceLocation, Codec<? extends T>> registry) {
        return new ConditionDispatchCodec<>(registry).codec();
    }

    private static final class ConditionDispatchCodec<T> extends MapCodec<ConditionDispatch<T>> {
        private static final String CONDITION_KEY = "condition";
        private final Map<ResourceLocation, Codec<? extends T>> registry;

        ConditionDispatchCodec(Map<ResourceLocation, Codec<? extends T>> registry) {
            this.registry = registry;
        }

        @Override
        public <U> DataResult<ConditionDispatch<T>> decode(DynamicOps<U> ops, MapLike<U> input) {
            U conditionValue = input.get(CONDITION_KEY);
            if (conditionValue == null) {
                return DataResult.error(() -> "Missing required field: 'condition'");
            }
            BoolMolangExp condition = BoolMolangExp.CODEC.parse(ops, conditionValue).getOrThrow(false, msg -> {
                throw new IllegalStateException(msg);
            });
            Optional<Pair<U, U>> typeEntry = input.entries().filter(pair -> {
                String key = ops.getStringValue(pair.getFirst()).result().orElse("");
                return !key.equals(CONDITION_KEY);
            }).findFirst();

            if (typeEntry.isEmpty()) {
                return DataResult.error(() -> "Expected exactly one type field, found none");
            }

            U typeKey = typeEntry.get().getFirst();
            U typeValue = typeEntry.get().getSecond();
            ResourceLocation typeId;
            try {
                String idStr = ops.getStringValue(typeKey).getOrThrow(false, msg -> {
                    throw new IllegalStateException(msg);
                });
                typeId = ResourceLocation.parse(idStr);
            } catch (Exception e) {
                return DataResult.error(() -> "Invalid type id: " + e.getMessage());
            }
            Codec<? extends T> valueCodec = registry.get(typeId);
            if (valueCodec == null) {
                return DataResult.error(() -> "Unknown type: " + typeId);
            }
            T value = valueCodec.parse(ops, typeValue).getOrThrow(false, msg -> {
                throw new IllegalStateException(msg);
            });
            return DataResult.success(new ConditionDispatch<>(condition, value));
        }

        @Override
        public <U> RecordBuilder<U> encode(ConditionDispatch<T> input, DynamicOps<U> ops, RecordBuilder<U> prefix) {
            return BoolMolangExp.CODEC.fieldOf(CONDITION_KEY).encode(input.condition(), ops, prefix);
        }

        @Override
        public <U> Stream<U> keys(DynamicOps<U> ops) {
            return Stream.concat(
                    Stream.of(ops.createString(CONDITION_KEY)),
                    registry.keySet().stream().map(id -> ops.createString(id.toString()))
            );
        }
    }
}

package org.mesdag.particlestorm.data.event;

import com.mojang.serialization.*;
import org.mesdag.particlestorm.data.molang.MolangInstance;

import java.util.stream.Stream;

public interface IEventNode {
    Codec<IEventNode> CODEC = null; // todo

    default void execute(MolangInstance instance) {}

    MapCodec<? extends IEventNode> codec();

    String name();

    @SuppressWarnings("all")
    private static <T> DataResult<MapCodecImpl<T>> safeCastToImpl(MapCodec<T> value) {
        return value instanceof MapCodecImpl reference
                ? DataResult.success(reference)
                : DataResult.error(() -> "Error");
    }

    @SuppressWarnings("all")
    private static <T, E extends T> MapCodec<T> safeCastToCodec(MapCodec<E> value) {
        return (MapCodec<T>) value;
    }

    @SuppressWarnings("all")
    static <T extends IEventNode> MapCodec<T> getMapCodec(String name) {
        if ("sequence".equals(name)) {
            return (MapCodec<T>) EventSequence.CODEC;
        } else if ("particle_effect".equals(name)) {
            return (MapCodec<T>) ParticleEffect.CODEC;
        } else if ("sound_effect".equals(name)) {
            return (MapCodec<T>) SoundEffect.CODEC;
        }
        return (MapCodec<T>) EventSubpart.CODEC;
    }

    class MapCodecImpl<A> extends MapCodec<A> {
        private final String name;
        private final MapCodec<A> prev;

        public MapCodecImpl(String name, MapCodec<A> prev) {
            this.name = name;
            this.prev = prev;
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return prev.keys(ops);
        }

        @Override
        public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
            return prev.decode(ops, input);
        }

        @Override
        public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return prev.encode(input, ops, prefix);
        }

        public String name() {
            return name;
        }
    }
}

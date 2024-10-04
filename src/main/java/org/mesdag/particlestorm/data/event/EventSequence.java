package org.mesdag.particlestorm.data.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record EventSequence(List<IEventNode> nodes) implements IEventNode {
    public static final MapCodec<EventSequence> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.list(IEventNode.CODEC).fieldOf("sequence").forGetter(EventSequence::nodes)
    ).apply(instance, EventSequence::new));

    @Override
    public MapCodec<EventSequence> codec() {
        return CODEC;
    }

    @Override
    public String name() {
        return "sequence";
    }
}

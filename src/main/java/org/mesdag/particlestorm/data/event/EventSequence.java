package org.mesdag.particlestorm.data.event;

import com.mojang.serialization.Codec;

import java.util.List;
import java.util.Map;

public record EventSequence(List<Map<String, IEventNode>> nodes) implements IEventNode {
    public static final Codec<EventSequence> CODEC = IEventNode.CODEC.listOf().xmap(EventSequence::new, EventSequence::nodes);
}

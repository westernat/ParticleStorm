package org.mesdag.particlestorm.data.event;

import com.mojang.serialization.Codec;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.api.MolangInstance;

import java.util.List;
import java.util.Map;

public record EventSequence(List<Map<String, IEventNode>> nodes) implements IEventNode {
    public static final Codec<EventSequence> CODEC = IEventNode.CODEC.listOf().xmap(EventSequence::new, EventSequence::nodes);

    @Override
    public void execute(MolangInstance instance) {
        for (Map<String, IEventNode> node1 : nodes) {
            node1.forEach((name, node) -> node.execute(instance));
        }
    }
}

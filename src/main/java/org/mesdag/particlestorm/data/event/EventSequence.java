package org.mesdag.particlestorm.data.event;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.api.MolangInstance;

import java.util.List;
import java.util.Map;

public record EventSequence(List<Map<String, IEventNode>> nodes) implements IEventNode {
    @Override
    public void execute(MolangInstance instance) {
        for (Map<String, IEventNode> nodeMap : nodes) {
            for (IEventNode node : nodeMap.values()) {
                node.execute(instance);
            }
        }
    }

    public static EventSequence fromJson(JsonElement element) {
        ImmutableList.Builder<Map<String, IEventNode>> builder = ImmutableList.builder();
        for (JsonElement jsonElement : element.getAsJsonArray()) {
            ImmutableMap.Builder<String, IEventNode> builder1 = ImmutableMap.builder();
            for (Map.Entry<String, JsonElement> entry : jsonElement.getAsJsonObject().entrySet()) {
                String key = entry.getKey();
                builder1.put(key, IEventNode.getDeserializer(key).fromJson(entry.getValue()));
            }
            builder.add(builder1.build());
        }
        return new EventSequence(builder.build());
    }
}

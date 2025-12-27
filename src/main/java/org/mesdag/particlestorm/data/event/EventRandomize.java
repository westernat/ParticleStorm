package org.mesdag.particlestorm.data.event;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraft.util.Tuple;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.api.MolangInstance;

import java.util.*;

public final class EventRandomize implements IEventNode {
    public final List<Map<String, IEventNode>> nodes;

    public final List<Tuple<Float, Map<String, IEventNode>>> sortedNodes;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public EventRandomize(List<Map<String, IEventNode>> nodes) {
        this.nodes = nodes;

        this.sortedNodes = new ArrayList<>();
        float allWeights = 0.0F;
        float[] cachedWeight = new float[nodes.size()];
        Map[] cachedNode = new Map[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            Map<String, IEventNode> node = new Hashtable<>(nodes.get(i));
            float weight = ((Weight) node.remove("weight")).value;
            cachedWeight[i] = weight;
            cachedNode[i] = node;
            allWeights += weight;
        }
        for (int i = 0; i < nodes.size(); i++) {
            sortedNodes.add(new Tuple<>(cachedWeight[i] / allWeights, cachedNode[i]));
        }
        sortedNodes.sort(Comparator.comparing(Tuple::getA));
    }

    @Override
    public void execute(MolangInstance instance) {
        float random = instance.getLevel().random.nextFloat();
        for (Tuple<Float, Map<String, IEventNode>> tuple : sortedNodes) {
            if (random < tuple.getA()) {
                for (IEventNode node : tuple.getB().values()) {
                    node.execute(instance);
                }
                break;
            }
        }
    }

    @Override
    public String toString() {
        return "EventRandomize[" + "nodes=" + nodes + ']';
    }

    public static EventRandomize fromJson(JsonElement element) {
        ImmutableList.Builder<Map<String, IEventNode>> builder = ImmutableList.builder();
        for (JsonElement jsonElement : element.getAsJsonArray()) {
            ImmutableMap.Builder<String, IEventNode> builder1 = ImmutableMap.builder();
            for (Map.Entry<String, JsonElement> entry : jsonElement.getAsJsonObject().entrySet()) {
                String key = entry.getKey();
                builder1.put(key, IEventNode.getDeserializer(key).fromJson(entry.getValue()));
            }
            builder.add(builder1.build());
        }
        return new EventRandomize(builder.build());
    }

    public record Weight(int value) implements IEventNode {
        @Override
        public void execute(MolangInstance instance) {}

        public static Weight fromJson(JsonElement element) {
            int i = element.getAsInt();
            if (i <= 0) {
                throw new JsonParseException("Value must be positive: " + i);
            }
            return new Weight(i);
        }
    }
}

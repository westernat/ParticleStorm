package org.mesdag.particlestorm.data.event;

import com.mojang.serialization.Codec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Tuple;
import org.mesdag.particlestorm.data.molang.MolangInstance;

import java.util.*;

public final class EventRandomize implements IEventNode {
    public static final Codec<EventRandomize> CODEC = Codec.dispatchedMap(Codec.STRING, name -> {
        Codec<IEventNode> codec = MAP.get(name);
        if (codec == null) return EventLog.CODEC;
        return codec;
    }).listOf().xmap(EventRandomize::new, eventRandomize -> eventRandomize.nodes);
    public final List<Map<String, IEventNode>> nodes;

    public final ArrayList<Tuple<Float, Map<String, IEventNode>>> sortedNodes;

    public EventRandomize(List<Map<String, IEventNode>> nodes) {
        this.nodes = nodes;

        this.sortedNodes = new ArrayList<>();
        float allWeights = 0.0F;
        float[] cachedWeight = new float[nodes.size()];
        Map[] cachedNode = new Map[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            Hashtable<String, IEventNode> node = new Hashtable<>(nodes.get(i));
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
                tuple.getB().forEach((name, node) -> node.execute(instance));
                break;
            }
        }
    }

    @Override
    public String toString() {
        return "EventRandomize[" + "nodes=" + nodes + ']';
    }

    public record Weight(int value) implements IEventNode {
        public static final Codec<Weight> CODEC = ExtraCodecs.POSITIVE_INT.xmap(Weight::new, Weight::value);

        @Override
        public void execute(MolangInstance instance) {}
    }
}

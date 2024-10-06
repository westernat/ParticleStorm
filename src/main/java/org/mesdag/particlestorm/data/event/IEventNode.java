package org.mesdag.particlestorm.data.event;

import com.mojang.serialization.Codec;
import org.mesdag.particlestorm.data.molang.MolangInstance;

import java.util.Hashtable;
import java.util.Map;

public interface IEventNode {
    Codec<Map<String, IEventNode>> CODEC = Codec.dispatchedMap(Codec.STRING, IEventNode::getCodec);
    Hashtable<String, Codec<IEventNode>> MAP = new Hashtable<>();

    void execute(MolangInstance instance);

    @SuppressWarnings("unchecked")
    static <T extends IEventNode> Codec<T> getCodec(String name) {
        Codec<T> codec = (Codec<T>) MAP.get(name);
        if (codec == null) return (Codec<T>) EventLog.CODEC;
        return codec;
    }

    static void register(String name, Codec<? extends IEventNode> codec) {
        MAP.put(name, (Codec<IEventNode>) codec);
    }
}

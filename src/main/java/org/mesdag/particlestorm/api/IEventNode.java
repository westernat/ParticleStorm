package org.mesdag.particlestorm.api;

import org.mesdag.particlestorm.data.event.EventLog;

import java.util.Hashtable;
import java.util.Map;

public interface IEventNode {
    Map<String, Deserializer<IEventNode>> MAP = new Hashtable<>();

    void execute(MolangInstance instance);

    @SuppressWarnings("unchecked")
    static <T extends IEventNode> Deserializer<T> getDeserializer(String name) {
        Deserializer<T> codec = (Deserializer<T>) MAP.get(name);
        if (codec == null) return (Deserializer<T>) EventLog.CODEC;
        return codec;
    }

    @SuppressWarnings("unchecked")
    static void register(String name, Deserializer<? extends IEventNode> codec) {
        MAP.put(name, (Deserializer<IEventNode>) codec);
    }
}

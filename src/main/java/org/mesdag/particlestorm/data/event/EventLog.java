package org.mesdag.particlestorm.data.event;

import com.mojang.serialization.Codec;

public record EventLog(String log) implements IEventNode {
    public static final Codec<EventLog> CODEC = Codec.STRING.xmap(EventLog::new, EventLog::log);
}

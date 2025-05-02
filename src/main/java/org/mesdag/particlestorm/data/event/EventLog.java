package org.mesdag.particlestorm.data.event;

import com.mojang.serialization.Codec;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.api.MolangInstance;

public record EventLog(String log) implements IEventNode {
    public static final Codec<EventLog> CODEC = Codec.STRING.xmap(EventLog::new, EventLog::log);

    @Override
    public void execute(MolangInstance instance) {
        ParticleStorm.LOGGER.info("{}[{}]: {}", instance.getIdentity(), instance.getPosition(), log);
    }
}

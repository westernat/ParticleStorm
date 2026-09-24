package org.mesdag.particlestorm.data.event;

import com.mojang.serialization.Codec;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.MolangInstance;

public record EventLog(String log) implements IEventNode {
    public static final Codec<EventLog> CODEC = Codec.STRING.xmap(EventLog::new, EventLog::log);

    @Override
    public void execute(MolangInstance instance) {
        Vec3 position;
        if (instance instanceof IMolangParticleInstance particle) {
            Vector3f world = particle.getWorldPosition(new Vector3f(), 1.0F);
            position = new Vec3(world.x, world.y, world.z);
        } else {
            position = instance.getPosition();
        }
        ParticleStorm.LOGGER.info("{}[{}]: {}", instance.getIdentity(), position, log);
    }
}

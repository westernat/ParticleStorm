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

    private static final Vector3f vector3f = new Vector3f();

    @Override
    public void execute(MolangInstance instance) {
        if (instance instanceof IMolangParticleInstance p) {
            p.getEmitter().local2World(vector3f.set((float) p.getX(), (float) p.getY(), (float) p.getZ()), 1);
        } else {
            Vec3 pos = instance.getPosition();
            vector3f.set(pos.x, pos.y, pos.z);
        }
        ParticleStorm.LOGGER.info("{}[{},{},{}]: {}", instance.getIdentity(), vector3f.x, vector3f.y, vector3f.z, log);
    }
}

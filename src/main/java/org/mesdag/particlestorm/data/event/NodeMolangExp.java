package org.mesdag.particlestorm.data.event;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.function.Function;

public final class NodeMolangExp extends MolangExp implements IEventNode {
    public static final Codec<NodeMolangExp> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("exp").forGetter(NodeMolangExp::getExpStr),
            Codec.BOOL.optionalFieldOf("log", false).forGetter(NodeMolangExp::shouldLog)
    ).apply(instance, NodeMolangExp::new));
    public static final Codec<NodeMolangExp> CODEC = Codec.either(DIRECT_CODEC, Codec.STRING).xmap(
            either -> either.map(Function.identity(), s -> new NodeMolangExp(s, false)),
            e -> e.log ? Either.right(e.expStr) : Either.left(e)
    );

    private final boolean log;

    public NodeMolangExp(String expStr, boolean log) {
        super(expStr);
        this.log = log;
    }

    public boolean shouldLog() {
        return log;
    }

    private static final Vector3f vector3f = new Vector3f();

    @Override
    public void execute(MolangInstance instance) {
        if (initialized()) {
            double v = variable.get(instance);
            if (log) {
                if (instance instanceof IMolangParticleInstance p) {
                    p.getEmitter().local2World(vector3f.set((float) p.getX(), (float) p.getY(), (float) p.getZ()), 1);
                } else {
                    Vec3 pos = instance.getPosition();
                    vector3f.set(pos.x, pos.y, pos.z);
                }
                ParticleStorm.LOGGER.info("{}[{},{},{}]: {}={}", instance.getIdentity(), vector3f.x, vector3f.y, vector3f.z, expStr, v);
            }
        }
    }
}

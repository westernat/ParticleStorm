package org.mesdag.particlestorm.data.event;

import com.mojang.serialization.Codec;
import org.mesdag.particlestorm.data.molang.MolangInstance;

import java.util.Map;

public interface IEventNode {
    Codec<Map<String, IEventNode>> CODEC = Codec.dispatchedMap(Codec.STRING, IEventNode::getCodec);

    default void execute(MolangInstance instance) {}

    @SuppressWarnings("unchecked")
    static <T extends IEventNode> Codec<T> getCodec(String name) {
        if ("sequence".equals(name)) return (Codec<T>) EventSequence.CODEC;
        if ("randomize".equals(name)) return (Codec<T>) EventRandomize.CODEC;
        if ("particle_effect".equals(name)) return (Codec<T>) ParticleEffect.CODEC.codec();
        if ("sound_effect".equals(name)) return (Codec<T>) SoundEffect.CODEC.codec();
        if ("expression".equals(name)) return (Codec<T>) NodeMolangExp.CODEC;
        return (Codec<T>) EventLog.CODEC;
    }
}

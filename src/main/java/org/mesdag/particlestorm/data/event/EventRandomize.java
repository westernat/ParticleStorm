package org.mesdag.particlestorm.data.event;

import com.mojang.serialization.Codec;
import net.minecraft.util.ExtraCodecs;

import java.util.List;
import java.util.Map;

public record EventRandomize(List<Map<String, IEventNode>> nodes) implements IEventNode {
    public static final Codec<EventRandomize> CODEC = Codec.dispatchedMap(Codec.STRING, name -> {
        if ("weight".equals(name)) return Weight.CODEC;
        if ("sequence".equals(name)) return EventSequence.CODEC;
        if ("randomize".equals(name)) return EventRandomize.CODEC;
        if ("particle_effect".equals(name)) return ParticleEffect.CODEC.codec();
        if ("sound_effect".equals(name)) return SoundEffect.CODEC.codec();
        if ("expression".equals(name)) return NodeMolangExp.CODEC;
        return EventLog.CODEC;
    }).listOf().xmap(EventRandomize::new, EventRandomize::nodes);

    public record Weight(int value) implements IEventNode {
        public static final Codec<Weight> CODEC = ExtraCodecs.POSITIVE_INT.xmap(Weight::new, Weight::value);
    }
}

package org.mesdag.particlestorm.data.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.mesdag.particlestorm.data.molang.MolangExp;

public record EventSubpart(MolangExp expression, String log) implements IEventNode {
    public static final MapCodec<EventSubpart> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            MolangExp.CODEC.fieldOf("expression").orElse(MolangExp.EMPTY).forGetter(EventSubpart::expression),
            Codec.STRING.fieldOf("log").orElse("").forGetter(EventSubpart::log)
    ).apply(instance, EventSubpart::new));

    @Override
    public MapCodec<EventSubpart> codec() {
        return CODEC;
    }

    @Override
    public String name() {
        return "subpart";
    }
}

package org.mesdag.particlestorm.data.event;

import com.mojang.serialization.MapCodec;

public record EventRandomize() implements IEventNode {
    @Override
    public MapCodec<EventRandomize> codec() {
        return null;
    }

    @Override
    public String name() {
        return "randomize";
    }
}

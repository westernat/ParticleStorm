package org.mesdag.particlestorm.api;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import com.mojang.serialization.Codec;

public class RegisterCustomEventNodeEvent extends Event implements IModBusEvent {
    public RegisterCustomEventNodeEvent() {
    }

    public void register(String name, Codec<? extends IEventNode> codec) {
        IEventNode.register(name, codec);
    }
}

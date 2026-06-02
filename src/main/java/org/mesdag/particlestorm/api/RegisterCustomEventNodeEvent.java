package org.mesdag.particlestorm.api;

import com.mojang.serialization.Codec;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

public class RegisterCustomEventNodeEvent extends Event implements IModBusEvent {
    public RegisterCustomEventNodeEvent() {}

    public void register(String name, Codec<? extends IEventNode> codec) {
        IEventNode.register(name, codec);
    }
}

package org.mesdag.particlestorm.network;

import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.mesdag.particlestorm.ParticleStorm;

public final class NetworkProxy {
    public static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ParticleStorm.asResource("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(packetId++, EmitterAttachPacketS2C.class, EmitterAttachPacketS2C::encode, EmitterAttachPacketS2C::decode, EmitterAttachPacketS2C::handle);
        CHANNEL.registerMessage(packetId++, EmitterCreationPacketS2C.class, EmitterCreationPacketS2C::encode, EmitterCreationPacketS2C::decode, EmitterCreationPacketS2C::handle);
        CHANNEL.registerMessage(packetId++, EmitterRemovalPacket.class, EmitterRemovalPacket::encode, EmitterRemovalPacket::decode, EmitterRemovalPacket::handle);
        CHANNEL.registerMessage(packetId++, EmitterSynchronizePacket.class, EmitterSynchronizePacket::encode, EmitterSynchronizePacket::decode, EmitterSynchronizePacket::handle);
    }
}

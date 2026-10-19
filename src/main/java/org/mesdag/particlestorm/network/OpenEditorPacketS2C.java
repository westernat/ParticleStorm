package org.mesdag.particlestorm.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.editor.EditorScreen;

public enum OpenEditorPacketS2C implements CustomPacketPayload {
    INSTANCE;
    public static final Type<OpenEditorPacketS2C> TYPE = new Type<>(ParticleStorm.asResource("open_editor"));
    public static final StreamCodec<ByteBuf, OpenEditorPacketS2C> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<OpenEditorPacketS2C> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> EditorScreen.open());
    }

    public static void sendToClient(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, INSTANCE);
    }
}

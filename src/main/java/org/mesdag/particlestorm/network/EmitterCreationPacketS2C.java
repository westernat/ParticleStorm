package org.mesdag.particlestorm.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.PSDiagnostics;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.ParticleEmitter;

public record EmitterCreationPacketS2C(ResourceLocation id, Vector3f pos, MolangExp expression, int entityId) implements FabricPacket {
    public static final PacketType<EmitterCreationPacketS2C> TYPE = PacketType.create(ParticleStorm.asResource("emitter_creation"), EmitterCreationPacketS2C::new);

    public EmitterCreationPacketS2C(FriendlyByteBuf buf) {
        this(buf.readResourceLocation(), new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat()), new MolangExp(buf.readUtf()), buf.readVarInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeFloat(pos.x);
        buf.writeFloat(pos.y);
        buf.writeFloat(pos.z);
        buf.writeUtf(expression.getExpStr());
        buf.writeVarInt(entityId);
    }

    @Override
    public PacketType<EmitterCreationPacketS2C> getType() {
        return TYPE;
    }

    public static void handleClient(EmitterCreationPacketS2C payload, Player player, PacketSender responseSender) {
        ResourceLocation resolved = PSGameClient.LOADER.resolveParticleId(payload.id);
        if (resolved == null) {
            PSDiagnostics.warn("ignoring unknown particle id from network requested={} knownIds={}", payload.id, PSGameClient.LOADER.suggestibleParticleIds());
            return;
        }
        try {
            Entity attached = payload.entityId > 0 ? player.level().getEntity(payload.entityId) : null;
            PSDiagnostics.infoFirstN("packet-client:" + resolved, 32, "client received emitter packet requested={} resolved={} pos=({}, {}, {}) expression={} entityId={} attached={}",
                    payload.id,
                    resolved,
                    payload.pos.x,
                    payload.pos.y,
                    payload.pos.z,
                    payload.expression == null ? "" : payload.expression.getExpStr(),
                    payload.entityId,
                    attached == null ? "none" : attached.getScoreboardName()
            );
            ParticleEmitter emitter = new ParticleEmitter(player.level(), new Vec3(payload.pos.x, payload.pos.y, payload.pos.z), resolved, payload.expression);
            if (attached != null) {
                emitter.attachEntity(attached);
            }
            PSGameClient.LOADER.addEmitter(emitter, false);
            PSDiagnostics.infoFirstN("packet-client-added:" + resolved, 32, "client added emitter runtimeId={} particle={} pos={} attached={}",
                    emitter.id,
                    emitter.particleId,
                    emitter.pos,
                    attached == null ? "none" : attached.getScoreboardName()
            );
        } catch (RuntimeException exception) {
            PSDiagnostics.error("failed to create emitter requested={} resolved={} pos=({}, {}, {}) expression={}",
                    payload.id,
                    resolved,
                    payload.pos.x,
                    payload.pos.y,
                    payload.pos.z,
                    payload.expression == null ? "" : payload.expression.getExpStr(),
                    exception
            );
        }
    }

    public static void sendToAll(ResourceLocation id, Vector3f pos, MolangExp expression, @Nullable Entity entity) {
        if (entity != null && entity.level().getServer() != null) {
            for (ServerPlayer player : entity.level().getServer().getPlayerList().getPlayers()) {
                sendToClient(player, id, pos, expression, entity);
            }
        }
    }

    public static void sendToClient(ServerPlayer player, ResourceLocation id, Vector3f pos, MolangExp expression, @Nullable Entity entity) {
        PSDiagnostics.infoFirstN("packet-send:" + id, 32, "server sending emitter packet viewer={} particle={} pos=({}, {}, {}) expression={} entity={}",
                player.getScoreboardName(),
                id,
                pos.x,
                pos.y,
                pos.z,
                expression == null ? "" : expression.getExpStr(),
                entity == null ? "none" : entity.getScoreboardName()
        );
        ServerPlayNetworking.send(player, new EmitterCreationPacketS2C(id, pos, expression, entity == null ? -1 : entity.getId()));
    }
}

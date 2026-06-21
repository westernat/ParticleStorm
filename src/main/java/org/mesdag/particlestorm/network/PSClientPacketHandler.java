package org.mesdag.particlestorm.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.mesdag.particlestorm.particle.ParticleEmitter;

public class PSClientPacketHandler {
    public static void handleEmitterAttach(EmitterAttachPacketS2C packet) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            ParticleEmitter emitter = MolangParticleEngine.INSTANCE.getEmitter(packet.particleId());
            Entity entity;
            if (emitter != null && (entity = player.level().getEntity(packet.entityId())) != null) {
                emitter.attachEntity(entity);
            }
        }
    }

    public static void handleEmitterCreation(EmitterCreationPacketS2C packet) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            ParticleEmitter emitter = new ParticleEmitter(player.level(), new Vec3(packet.pos()), packet.id(), packet.expression());
            if (packet.entityId() > 0) {
                emitter.attachEntity(player.level().getEntity(packet.entityId()));
            }
            MolangParticleEngine.INSTANCE.addEmitter(emitter);
        }
    }

    public static void handleEmitterRemoval(EmitterRemovalPacket packet) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            ParticleEmitter emitter = MolangParticleEngine.INSTANCE.removeEmitter(packet.id(), false);
            if (emitter == null) {
                player.displayClientMessage(Component.translatable("particle.notFound", packet.id()), false);
            } else {
                player.displayClientMessage(Component.translatable("commands.particlestorm.remove", emitter.particleId == null ? packet.id() : emitter.particleId.toString()), false);
            }
        }
    }

    public static void handleEmitterSynchronize(EmitterSynchronizePacket packet) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            MolangParticleEngine.INSTANCE.loadEmitter(player.level(), packet.id(), packet.tag());
        }
    }
}

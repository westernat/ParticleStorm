package org.mesdag.particlestorm.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.ParticleEmitter;

public class ClientProxy {
    public static void handleAttach(int particleId, int entityId) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            ParticleEmitter emitter = PSGameClient.LOADER.getEmitter(particleId);
            Entity entity;
            if (emitter != null && (entity = player.level().getEntity(entityId)) != null) {
                emitter.attachEntity(entity);
            }
        }
    }

    public static void handleCreation(ResourceLocation id, Vector3f pos, MolangExp expression, int entityId) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            ParticleEmitter emitter = new ParticleEmitter(player.level(), new Vec3(pos.x, pos.y, pos.z), id, expression);
            if (entityId > 0) {
                emitter.attachEntity(player.level().getEntity(entityId));
            }
            PSGameClient.LOADER.addEmitter(emitter, false);
        }
    }

    public static void handleRemove(int id) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            ParticleEmitter emitter = PSGameClient.LOADER.removeEmitter(id, false);
            if (emitter == null) {
                player.sendSystemMessage(Component.translatable("particle.notFound", id));
            } else {
                player.sendSystemMessage(Component.translatable("commands.particlestorm.remove", emitter.particleId == null ? id : emitter.particleId.toString()));
            }
        }
    }

    public static void handleSynchronize(int id, CompoundTag tag) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            PSGameClient.LOADER.loadEmitter(player.level(), id, tag);
        }
    }
}

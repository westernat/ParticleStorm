package org.mesdag.particlestorm;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.network.EmitterAttachPacketS2C;
import org.mesdag.particlestorm.network.EmitterCreationPacketS2C;
import org.mesdag.particlestorm.network.EmitterRemovalPacket;
import org.mesdag.particlestorm.network.EmitterSynchronizePacket;
import org.mesdag.particlestorm.particle.MolangParticleCommand;
import org.mesdag.particlestorm.particle.MolangParticleOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public final class ParticleStorm implements ModInitializer {
    public static final String MODID = "particlestorm";
    public static final Logger LOGGER = LoggerFactory.getLogger("ParticleStorm");
    public static final ParticleType<MolangParticleOption> MOLANG = new ParticleType<>(false, MolangParticleOption.DESERIALIZER) {
        @Override
        public Codec<MolangParticleOption> codec() {
            return MolangParticleOption.CODEC;
        }
    };
    public static final Codec<List<String>> STRING_LIST_CODEC = Codec.either(Codec.STRING, Codec.STRING.listOf()).xmap(
            either -> either.map(Collections::singletonList, Function.identity()),
            l -> l.size() == 1 ? Either.left(l.get(0)) : Either.right(l)
    );

    @Override
    public void onInitialize() {
        PSClientConfigs.onLoad();
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, asResource("molang"), MOLANG);
        registerPayloads();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> MolangParticleCommand.register(dispatcher));
        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> EmitterSynchronizePacket.syncSavedEmitters(listener.getPlayer()));
    }

    private static void registerPayloads() {
        ServerPlayNetworking.registerGlobalReceiver(EmitterRemovalPacket.TYPE, EmitterRemovalPacket::handleServer);
        ServerPlayNetworking.registerGlobalReceiver(EmitterSynchronizePacket.TYPE, EmitterSynchronizePacket::handleServer);
    }

    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(MODID, path);
    }
}

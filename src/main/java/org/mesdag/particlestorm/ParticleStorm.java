package org.mesdag.particlestorm;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
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
    public static final ParticleType<MolangParticleOption> MOLANG = new ParticleType<>(false) {
        @Override
        public MapCodec<MolangParticleOption> codec() {
            return MolangParticleOption.CODEC;
        }

        @Override
        public StreamCodec<ByteBuf, MolangParticleOption> streamCodec() {
            return MolangParticleOption.STREAM_CODEC;
        }
    };
    public static final Codec<List<String>> STRING_LIST_CODEC = Codec.either(Codec.STRING, Codec.STRING.listOf()).xmap(
            either -> either.map(Collections::singletonList, Function.identity()),
            l -> l.size() == 1 ? Either.left(l.getFirst()) : Either.right(l)
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
        registerClientbound(EmitterCreationPacketS2C.TYPE, EmitterCreationPacketS2C.STREAM_CODEC);
        registerClientbound(EmitterAttachPacketS2C.TYPE, EmitterAttachPacketS2C.STREAM_CODEC);
        registerClientbound(EmitterRemovalPacket.TYPE, EmitterRemovalPacket.STREAM_CODEC);
        registerClientbound(EmitterSynchronizePacket.TYPE, EmitterSynchronizePacket.STREAM_CODEC);
        registerServerbound(EmitterRemovalPacket.TYPE, EmitterRemovalPacket.STREAM_CODEC);
        registerServerbound(EmitterSynchronizePacket.TYPE, EmitterSynchronizePacket.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(EmitterRemovalPacket.TYPE, EmitterRemovalPacket::handleServer);
        ServerPlayNetworking.registerGlobalReceiver(EmitterSynchronizePacket.TYPE, EmitterSynchronizePacket::handleServer);
    }

    private static <T extends CustomPacketPayload> void registerClientbound(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {
        PayloadTypeRegistry.clientboundPlay().register(type, codec);
    }

    private static <T extends CustomPacketPayload> void registerServerbound(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {
        PayloadTypeRegistry.serverboundPlay().register(type, codec);
    }

    public static Identifier asResource(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}

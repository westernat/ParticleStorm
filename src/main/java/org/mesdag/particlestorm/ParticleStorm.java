package org.mesdag.particlestorm;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
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

@Mod(ParticleStorm.MODID)
public final class ParticleStorm {
    public static final String MODID = "particlestorm";
    public static final Logger LOGGER = LoggerFactory.getLogger("ParticleStorm");
    public static final boolean GECKOLIB_LOADED = ModList.get().isLoaded("geckolib");

    private static final DeferredRegister<ParticleType<?>> REGISTER = DeferredRegister.create(Registries.PARTICLE_TYPE, MODID);
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

    public ParticleStorm(IEventBus bus, ModContainer container) {
        PSClientConfigs.onLoad();
        REGISTER.register("molang", () -> MOLANG);
        REGISTER.register(bus);
        bus.addListener(ParticleStorm::registerPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(ParticleStorm::registerCommands);
        NeoForge.EVENT_BUS.addListener(ParticleStorm::playerLoggedIn);
    }

    private static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        // The common registrar declares payload types and server-side handlers only.
        // Client-bound types must be declared here (playToClient/playBidirectional) so the
        // per-side RegisterClientPayloadHandlersEvent (see PSGameClient) can attach client
        // handlers; a type cannot be registered twice, so bidirectional payloads use
        // playBidirectional with a null client handler. Handlers run on the default main
        // thread: no explicit HandlerThread.NETWORK / enqueueWork is used.
        event.registrar("1")
                .playToClient(EmitterCreationPacketS2C.TYPE, EmitterCreationPacketS2C.STREAM_CODEC)
                .playToClient(EmitterAttachPacketS2C.TYPE, EmitterAttachPacketS2C.STREAM_CODEC)
                .playBidirectional(EmitterRemovalPacket.TYPE, EmitterRemovalPacket.STREAM_CODEC, EmitterRemovalPacket::handleServer, null)
                .playBidirectional(EmitterSynchronizePacket.TYPE, EmitterSynchronizePacket.STREAM_CODEC, EmitterSynchronizePacket::handleServer, null)
        ;
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        MolangParticleCommand.register(event.getDispatcher());
    }

    private static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EmitterSynchronizePacket.syncSavedEmitters(player);
        }
    }

    public static Identifier asResource(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}

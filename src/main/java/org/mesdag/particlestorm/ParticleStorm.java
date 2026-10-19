package org.mesdag.particlestorm;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import org.mesdag.particlestorm.network.EmitterAttachPacketS2C;
import org.mesdag.particlestorm.network.EmitterCreationPacketS2C;
import org.mesdag.particlestorm.network.EmitterRemovalPacketS2C;
import org.mesdag.particlestorm.network.OpenEditorPacketS2C;
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
    public static final boolean GECKOLIB_LOADED = LoadingModList.get().getModFileById("geckolib") != null;
    public static final boolean SODIUM_LOADED = LoadingModList.get().getModFileById("sodium") != null;
    public static final boolean IRIS_LOADED = LoadingModList.get().getModFileById("iris") != null;
    public static final boolean DEBUG = Boolean.getBoolean("particlestorm.debug") && GECKOLIB_LOADED;

    private static final DeferredRegister<ParticleType<?>> REGISTER = DeferredRegister.create(Registries.PARTICLE_TYPE, MODID);
    public static final DeferredHolder<ParticleType<?>, ParticleType<MolangParticleOption>> MOLANG = registerParticleType(REGISTER, "molang");
    public static final Codec<List<String>> STRING_LIST_CODEC = Codec.either(Codec.STRING, Codec.STRING.listOf()).xmap(
            either -> either.map(Collections::singletonList, Function.identity()),
            l -> l.size() == 1 ? Either.left(l.getFirst()) : Either.right(l)
    );

    public ParticleStorm(IEventBus bus) {
        REGISTER.register(bus);
        registerGeoTest(bus);
        bus.addListener(ParticleStorm::registerPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(ParticleStorm::registerCommands);
    }

    private static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(EmitterCreationPacketS2C.TYPE, EmitterCreationPacketS2C.STREAM_CODEC, EmitterCreationPacketS2C::handle)
                .playToClient(EmitterAttachPacketS2C.TYPE, EmitterAttachPacketS2C.STREAM_CODEC, EmitterAttachPacketS2C::handle)
                .playToClient(OpenEditorPacketS2C.TYPE, OpenEditorPacketS2C.STREAM_CODEC, OpenEditorPacketS2C::handle)
                .playToClient(EmitterRemovalPacketS2C.TYPE, EmitterRemovalPacketS2C.STREAM_CODEC, EmitterRemovalPacketS2C::handle)
        ;
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        MolangParticleCommand.register(event.getDispatcher());
    }

    private static void registerGeoTest(IEventBus bus) {
        if (DEBUG) {
            GeckoLibHelper.registerStuffs(bus);
        }
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public static DeferredHolder<ParticleType<?>, ParticleType<MolangParticleOption>> registerParticleType(DeferredRegister<ParticleType<?>> register, String name) {
        return register.register(name, () -> new ParticleType<>(false) {
            @Override
            public MapCodec<MolangParticleOption> codec() {
                return MolangParticleOption.CODEC;
            }

            @Override
            public StreamCodec<ByteBuf, MolangParticleOption> streamCodec() {
                return MolangParticleOption.STREAM_CODEC;
            }
        });
    }
}

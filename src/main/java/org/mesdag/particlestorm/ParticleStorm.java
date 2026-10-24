package org.mesdag.particlestorm;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import org.mesdag.particlestorm.network.*;
import org.mesdag.particlestorm.particle.MolangParticleCommand;
import org.mesdag.particlestorm.particle.MolangParticleOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.mesdag.particlestorm.network.EmitterSynchronizePacket.KEY;

@Mod(ParticleStorm.MODID)
public final class ParticleStorm {
    public static final String MODID = "particlestorm";
    public static final Logger LOGGER = LoggerFactory.getLogger("ParticleStorm");
    public static final boolean GECKOLIB_LOADED = LoadingModList.get().getModFileById("geckolib") != null;
    public static final boolean OCULUS_LOADED = LoadingModList.get().getModFileById("oculus") != null;
    public static final boolean DEBUG = Boolean.getBoolean("particlestorm.debug") && GECKOLIB_LOADED;

    private static final DeferredRegister<ParticleType<?>> REGISTER = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MODID);
    public static final RegistryObject<ParticleType<MolangParticleOption>> MOLANG = registerParticleType(REGISTER, "molang");

    public static RegistryObject<ParticleType<MolangParticleOption>> registerParticleType(DeferredRegister<ParticleType<?>> register, String name) {
        return register.register(name, MolangParticleType::new);
    }

    static class MolangParticleType extends ParticleType<MolangParticleOption> {
        MolangParticleType() {
            super(false, MolangParticleOption.DESERIALIZER);
        }

        @Override
        public Codec<MolangParticleOption> codec() {
            return MolangParticleOption.CODEC;
        }
    }

    public static final Codec<List<String>> STRING_LIST_CODEC = Codec.either(Codec.STRING, Codec.STRING.listOf()).xmap(
            either -> either.map(Collections::singletonList, Function.identity()),
            l -> l.size() == 1 ? Either.left(l.get(0)) : Either.right(l)
    );

    public static final String NETWORK_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            asResource("main"),
            () -> NETWORK_VERSION,
            NETWORK_VERSION::equals,
            NETWORK_VERSION::equals
    );

    public ParticleStorm(FMLJavaModLoadingContext context) {
        IEventBus eventBus = context.getModEventBus();
        if (FMLEnvironment.dist.isClient()) {
            PSClientConfigs.register(context);
            MinecraftForge.EVENT_BUS.addListener(PSGameClient::clientNetwork$LoggingOut);
            MinecraftForge.EVENT_BUS.addListener(PSGameClient::clientTick$Pre);
            MinecraftForge.EVENT_BUS.addListener(PSGameClient::renderLevelStage);
        }
        REGISTER.register(eventBus);
        registerGeoTest(eventBus);
        registerNetwork();
        MinecraftForge.EVENT_BUS.addListener(ParticleStorm::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(ParticleStorm::playerLoggedIn);
    }

    private static void registerNetwork() {
        CHANNEL.messageBuilder(EmitterCreationPacketS2C.class, 0)
                .encoder(EmitterCreationPacketS2C::encode)
                .decoder(EmitterCreationPacketS2C::decode)
                .consumerMainThread(EmitterCreationPacketS2C::handle)
                .add();
        CHANNEL.messageBuilder(EmitterAttachPacketS2C.class, 1)
                .encoder(EmitterAttachPacketS2C::encode)
                .decoder(EmitterAttachPacketS2C::decode)
                .consumerMainThread(EmitterAttachPacketS2C::handle)
                .add();
        CHANNEL.messageBuilder(EmitterRemovalPacket.class, 2)
                .encoder(EmitterRemovalPacket::encode)
                .decoder(EmitterRemovalPacket::decode)
                .consumerMainThread(EmitterRemovalPacket::handle)
                .add();
        CHANNEL.messageBuilder(EmitterSynchronizePacket.class, 3)
                .encoder(EmitterSynchronizePacket::encode)
                .decoder(EmitterSynchronizePacket::decode)
                .consumerMainThread(EmitterSynchronizePacket::handle)
                .add();
        CHANNEL.messageBuilder(OpenEditorPacketS2C.class, 4)
                .encoder(OpenEditorPacketS2C::encode)
                .decoder(OpenEditorPacketS2C::decode)
                .consumerMainThread(OpenEditorPacketS2C::handle)
                .add();
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        MolangParticleCommand.register(event.getDispatcher());
    }

    private static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CompoundTag data = player.getPersistentData();
            if (data.contains(KEY)) {
                CompoundTag emitters = data.getCompound(KEY);
                for (String emitterId : emitters.getAllKeys()) {
                    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EmitterSynchronizePacket(Integer.parseInt(emitterId), emitters.getCompound(emitterId)));
                }
            }
        }
    }

    private static void registerGeoTest(IEventBus bus) {
        if (DEBUG) {
            GeckoLibHelper.registerStuffs(bus);
        }
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public static <A> MapCodec<A> lenientOptionalFieldOf(Codec<A> codec, String name, Supplier<? extends A> value) {
        return Codec.optionalField(name, codec).xmap(
                o -> o.orElseGet(value),
                a -> Objects.equals(a, value.get()) ? Optional.empty() : Optional.of(a)
        );
    }
}

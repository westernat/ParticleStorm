package org.mesdag.particlestorm;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.network.EmitterSynchronizePacket;
import org.mesdag.particlestorm.network.NetworkProxy;
import org.mesdag.particlestorm.particle.MolangParticleCommand;
import org.mesdag.particlestorm.particle.MolangParticleOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.mesdag.particlestorm.network.EmitterSynchronizePacket.KEY;

@Mod(ParticleStorm.MODID)
public class ParticleStorm {
    public static final String MODID = "particlestorm";
    public static final Logger LOGGER = LoggerFactory.getLogger("ParticleStorm");
    public static final boolean DEBUG = Boolean.getBoolean("particlestorm.debug") && LoadingModList.get().getModFileById("geckolib") != null;

    private static final DeferredRegister<ParticleType<?>> REGISTER = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MODID);
    public static final RegistryObject<ParticleType<MolangParticleOption>> MOLANG = registerParticleType(REGISTER, "molang");

    public ParticleStorm() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext context = ModLoadingContext.get();
        PSClientConfigs.register(context);
        REGISTER.register(bus);
        bus.addListener(ParticleStorm::fmlCommonSetup);
        MinecraftForge.EVENT_BUS.addListener(ParticleStorm::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(ParticleStorm::playerLoggedIn);
    }

    private static void fmlCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkProxy::register);
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        MolangParticleCommand.register(event.getDispatcher());
    }

    private static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CompoundTag data = player.getPersistentData();
            if (data.contains(KEY)) {
                CompoundTag emitters = data.getCompound(KEY);
                for (String id : emitters.getAllKeys()) {
                    NetworkProxy.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new EmitterSynchronizePacket(Integer.parseInt(id), emitters.getCompound(id))
                    );
                }
            }
        }
    }

    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(MODID, path);
    }

    public static RegistryObject<ParticleType<MolangParticleOption>> registerParticleType(DeferredRegister<ParticleType<?>> register, String name) {
        ParticleOptions.Deserializer<MolangParticleOption> deserializer = new ParticleOptions.Deserializer<>() {
            @Override
            public MolangParticleOption fromCommand(ParticleType<MolangParticleOption> particleType, StringReader reader) throws CommandSyntaxException {
                return new MolangParticleOption(ResourceLocation.read(reader));
            }

            @Override
            public MolangParticleOption fromNetwork(ParticleType<MolangParticleOption> particleType, FriendlyByteBuf buffer) {
                return new MolangParticleOption(buffer.readResourceLocation());
            }
        };
        return register.register(name, () -> new ParticleType<>(false, deserializer) {
            @Override
            public Codec<MolangParticleOption> codec() {
                return MolangParticleOption.CODEC;
            }
        });
    }

    public static List<String> getAsStringList(@Nullable JsonElement element) {
        if (element == null) return List.of();
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        if (element.isJsonArray()) {
            for (JsonElement jsonElement : element.getAsJsonArray()) {
                builder.add(jsonElement.getAsString());
            }
        } else if (element.isJsonPrimitive()) {
            builder.add(element.getAsString());
        } else {
            throw new JsonParseException("Not a(n) array or string: " + element);
        }
        return builder.build();
    }

    public static float positive(float value) {
        if (value <= 0) {
            throw new JsonParseException("Value must be positive: " + value);
        }
        return value;
    }
}

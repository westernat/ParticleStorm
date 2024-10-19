package org.mesdag.particlestorm;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.mesdag.particlestorm.integration.geckolib.TestBlock;
import org.mesdag.particlestorm.network.EmitterCreationPacketC2S;
import org.mesdag.particlestorm.network.EmitterRemovalPacket;
import org.mesdag.particlestorm.network.EmitterSynchronizePacket;
import org.mesdag.particlestorm.particle.MolangParticleCommand;
import org.mesdag.particlestorm.particle.MolangParticleOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.mesdag.particlestorm.network.EmitterSynchronizePacket.KEY;

@Mod(ParticleStorm.MODID)
public final class ParticleStorm {
    public static final String MODID = "particlestorm";
    public static final Logger LOGGER = LoggerFactory.getLogger("ParticleStorm");
    public static final boolean DEBUG = Boolean.getBoolean("particlestorm.debug");

    public static final DeferredRegister<ParticleType<?>> PARTICLE = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, MODID);
    public static final DeferredHolder<ParticleType<?>, ParticleType<MolangParticleOption>> MOLANG = PARTICLE.register("molang", () -> new ParticleType<MolangParticleOption>(false) {
        @Override
        public @NotNull MapCodec<MolangParticleOption> codec() {
            return MolangParticleOption.codec(this);
        }

        @Override
        public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, MolangParticleOption> streamCodec() {
            return MolangParticleOption.streamCodec(this);
        }
    });
    public static final Codec<List<String>> STRING_LIST_CODEC = Codec.either(Codec.STRING, Codec.list(Codec.STRING)).xmap(
            either -> either.map(Collections::singletonList, Function.identity()),
            l -> l.size() == 1 ? Either.left(l.getFirst()) : Either.right(l)
    );
    public static DeferredRegister<Block> BLOCK;
    public static DeferredRegister<BlockEntityType<?>> ENTITY;
    public static DeferredHolder<Block, Block> TEST;
    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<TestBlock.ExampleBlockEntity>> TEST_ENTITY;

    public ParticleStorm(IEventBus bus, ModContainer container) {
        PARTICLE.register(bus);
        registerGeoTest(bus);
        bus.addListener(ParticleStorm::registerPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(ParticleStorm::registerCommands);
        NeoForge.EVENT_BUS.addListener(ParticleStorm::playerLoggedIn);
    }

    private static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                EmitterCreationPacketC2S.TYPE,
                EmitterCreationPacketC2S.STREAM_CODEC,
                EmitterCreationPacketC2S::handle
        );
        registrar.playBidirectional(
                EmitterRemovalPacket.TYPE,
                EmitterRemovalPacket.STREAM_CODEC,
                EmitterRemovalPacket::handle
        );
        registrar.playBidirectional(
                EmitterSynchronizePacket.TYPE,
                EmitterSynchronizePacket.STREAM_CODEC,
                EmitterSynchronizePacket::handle
        );
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
                    PacketDistributor.sendToPlayer(player, new EmitterSynchronizePacket(Integer.parseInt(id), emitters.getCompound(id)));
                }
            }
        }
    }

    private static void registerGeoTest(IEventBus bus) {
        if (DEBUG) {
            BLOCK = DeferredRegister.create(BuiltInRegistries.BLOCK, MODID);
            ENTITY = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);
            TEST = BLOCK.register("test_block", TestBlock::new);
            TEST_ENTITY = ENTITY.register("test_entity", () -> BlockEntityType.Builder.of(TestBlock.ExampleBlockEntity::new, TEST.get()).build(null));
            BLOCK.register(bus);
            ENTITY.register(bus);
        }
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}

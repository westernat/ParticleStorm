package org.mesdag.particlestorm;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.mesdag.particlestorm.network.EmitterDiscardPacketC2S;
import org.mesdag.particlestorm.network.EmitterManualPacketC2S;
import org.mesdag.particlestorm.network.EmitterParticlePacketS2C;
import org.mesdag.particlestorm.particle.MolangParticleOption;
import org.mesdag.particlestorm.particle.ParticleEmitterEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

@Mod(ParticleStorm.MODID)
public final class ParticleStorm {
    public static final String MODID = "particlestorm";
    public static final Logger LOGGER = LoggerFactory.getLogger("ParticleStorm");

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<ParticleEmitterEntity>> PARTICLE_EMITTER = ENTITIES.register("particle_emitter", () -> EntityType.Builder.<ParticleEmitterEntity>of(ParticleEmitterEntity::new, MobCategory.MISC).fireImmune().sized(0, 0).clientTrackingRange(10).updateInterval(1).build("particlestorm:particle_emitter"));

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
            either -> either.map(Collections::singletonList, l -> l),
            l -> l.size() == 1 ? Either.left(l.getFirst()) : Either.right(l)
    );

    public ParticleStorm(IEventBus bus, ModContainer container) {
        ENTITIES.register(bus);
        PARTICLE.register(bus);
        bus.addListener(ParticleStorm::registerPayloadHandlers);
    }

    private static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                EmitterDiscardPacketC2S.TYPE,
                EmitterDiscardPacketC2S.STREAM_CODEC,
                EmitterDiscardPacketC2S::handle
        );
        registrar.playToServer(
                EmitterManualPacketC2S.TYPE,
                EmitterManualPacketC2S.STREAM_CODEC,
                EmitterManualPacketC2S::handle
        );
        registrar.playBidirectional(
                EmitterParticlePacketS2C.TYPE,
                EmitterParticlePacketS2C.STREAM_CODEC,
                EmitterParticlePacketS2C::handle
        );
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}

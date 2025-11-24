package org.mesdag.particlestorm.api;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.Contract;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.particle.ExtendMutableSpriteSet;
import org.mesdag.particlestorm.particle.ParticleEmitter;
import org.mesdag.particlestorm.particle.ParticlePreset;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegisterCustomParticleTypeEvent extends Event implements IModBusEvent {
    private static Map<ParticleType<?>, Provider<?>> map;
    private static ExtendMutableSpriteSet sprites;
    private final Map<ResourceLocation, ParticleEngine.MutableSpriteSet> spriteSets;

    private RegisterCustomParticleTypeEvent(Map<ResourceLocation, ParticleEngine.MutableSpriteSet> spriteSets) {
        this.spriteSets = spriteSets;
    }

    public void register(ParticleType<?> type, Provider<?> provider) {
        map.put(type, provider);
    }

    public void registerWithSprites(ParticleType<?> type, ProviderWithSprites<?> provider) {
        spriteSets.put(Objects.requireNonNull(BuiltInRegistries.PARTICLE_TYPE.getKey(type)), sprites);
        register(type, provider);
    }

    public void registerWithSprites(Holder<ParticleType<?>> holder, ProviderWithSprites<?> provider) {
        spriteSets.put(holder.unwrapKey().orElseThrow().location(), sprites);
        register(holder.value(), provider);
    }

    public static void postEvent(Map<ResourceLocation, ParticleEngine.MutableSpriteSet> spriteSets) {
        map = new HashMap<>();
        sprites = new ExtendMutableSpriteSet();
        ModLoader.postEvent(new RegisterCustomParticleTypeEvent(spriteSets));
    }

    public static <V extends Particle & IMolangParticleInstance> V createParticle(ParticleEmitter emitter) {
        Provider<?> provider = map.get(emitter.getPreset().type);
        if (provider == null) {
            throw new NullPointerException("Provider from '" + BuiltInRegistries.PARTICLE_TYPE.getKey(emitter.getPreset().type) + "' is not registered");
        }

        return (V) provider.create(emitter, PSGameClient.LOADER.id2Particle().get(emitter.particleId), (ClientLevel) emitter.level, emitter.getX(), emitter.getY(), emitter.getZ(), sprites);
    }

    @FunctionalInterface
    public interface Provider<V extends Particle & IMolangParticleInstance> {
        V create(ParticleEmitter emitter, ParticlePreset particlePreset, ClientLevel level, double x, double y, double z);

        /// @see Provider#create(ParticleEmitter, ParticlePreset, ClientLevel, double, double, double)
        @Contract("_, _, _, _, _, _, _ -> fail")
        default V create(ParticleEmitter emitter, ParticlePreset particlePreset, ClientLevel level, double x, double y, double z, ExtendMutableSpriteSet sprites) {
            throw new UnsupportedOperationException();
        }
    }

    @FunctionalInterface
    public interface ProviderWithSprites<V extends Particle & IMolangParticleInstance> extends Provider<V> {
        /// @see ProviderWithSprites#create(ParticleEmitter, ParticlePreset, ClientLevel, double, double, double, ExtendMutableSpriteSet)
        @Contract("_, _, _, _, _, _ -> fail")
        default V create(ParticleEmitter emitter, ParticlePreset particlePreset, ClientLevel level, double x, double y, double z) {
            throw new UnsupportedOperationException();
        }

        @Override
        V create(ParticleEmitter emitter, ParticlePreset particlePreset, ClientLevel level, double x, double y, double z, ExtendMutableSpriteSet sprites);
    }
}

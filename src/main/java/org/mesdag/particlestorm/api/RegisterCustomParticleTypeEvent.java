package org.mesdag.particlestorm.api;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.data.AtlasIds;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.PSModClient;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.particle.ExtendMutableSpriteSet;
import org.mesdag.particlestorm.particle.ParticleEmitter;
import org.mesdag.particlestorm.particle.ParticlePreset;

import java.util.HashMap;
import java.util.Map;

public class RegisterCustomParticleTypeEvent {
    private static final Map<ParticleType<?>, Provider<?>> PROVIDERS = new HashMap<>();
    private static final ExtendMutableSpriteSet SPRITES = new ExtendMutableSpriteSet();

    public void register(ParticleType<?> type, Provider<?> provider) {
        PROVIDERS.put(type, provider);
    }

    public void registerWithSprites(ParticleType<?> type, ProviderWithSprites<?> provider) {
        register(type, provider);
    }

    public static void registerDefaults() {
        PROVIDERS.clear();
        SPRITES.clear();
        PSModClient.registerCustomParticleType(new RegisterCustomParticleTypeEvent());
    }

    public static void bindSprites(Map<Identifier, DefinedParticleEffect> effects) {
        SPRITES.clear();
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES);
        SPRITES.bindMissing(atlas.getSprite(MissingTextureAtlasSprite.getLocation()));
        for (DefinedParticleEffect effect : effects.values()) {
            SPRITES.addSprite(effect.description.parameters().getTexture());
        }
    }

    @SuppressWarnings("unchecked")
    public static <V extends Particle & IMolangParticleInstance> V createParticle(ParticleEmitter emitter) {
        Provider<?> provider = PROVIDERS.get(emitter.getPreset().type);
        if (provider == null) {
            throw new NullPointerException("Provider from '" + BuiltInRegistries.PARTICLE_TYPE.getKey(emitter.getPreset().type) + "' is not registered");
        }

        return (V) provider.create(emitter, PSGameClient.LOADER.id2Particle().get(emitter.particleId), (ClientLevel) emitter.level, emitter.getX(), emitter.getY(), emitter.getZ(), SPRITES);
    }

    @FunctionalInterface
    public interface Provider<V extends Particle & IMolangParticleInstance> {
        V create(ParticleEmitter emitter, ParticlePreset particlePreset, ClientLevel level, double x, double y, double z);

        default V create(ParticleEmitter emitter, ParticlePreset particlePreset, ClientLevel level, double x, double y, double z, ExtendMutableSpriteSet sprites) {
            return create(emitter, particlePreset, level, x, y, z);
        }
    }

    @FunctionalInterface
    public interface ProviderWithSprites<V extends Particle & IMolangParticleInstance> extends Provider<V> {
        @Override
        default V create(ParticleEmitter emitter, ParticlePreset particlePreset, ClientLevel level, double x, double y, double z) {
            throw new UnsupportedOperationException();
        }

        @Override
        V create(ParticleEmitter emitter, ParticlePreset particlePreset, ClientLevel level, double x, double y, double z, ExtendMutableSpriteSet sprites);
    }
}

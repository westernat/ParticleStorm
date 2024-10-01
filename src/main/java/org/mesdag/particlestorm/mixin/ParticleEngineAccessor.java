package org.mesdag.particlestorm.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(ParticleEngine.class)
public interface ParticleEngineAccessor {
    @Accessor("spriteSets")
    Map<ResourceLocation, ParticleEngine.MutableSpriteSet> spriteSets();

    @Accessor("providers")
    Map<ResourceLocation, ParticleProvider<?>> providers();

    @Accessor("textureAtlas")
    TextureAtlas textureAtlas();

    @Accessor("trackedParticleCounts")
    Object2IntOpenHashMap<ParticleGroup> trackedParticleCounts();

    @Invoker
    <T extends ParticleOptions> Particle callMakeParticle(T particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed);
}

package org.mesdag.particlestorm.mixin;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ParticleEngine.class)
public interface ParticleEngineAccessor {
    @Accessor("spriteSets")
    Map<ResourceLocation, ParticleEngine.MutableSpriteSet> spriteSets();

    @Accessor("providers")
    Map<ResourceLocation, ParticleProvider<?>> providers();

    @Accessor("textureAtlas")
    TextureAtlas textureAtlas();
}

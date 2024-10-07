package org.mesdag.particlestorm.mixinauxi;

import net.minecraft.client.renderer.texture.SpriteLoader;

import java.util.function.Consumer;

public interface ITextureAtlas {
    void particlestorm$consume(Consumer<SpriteLoader.Preparations> consumer);
}

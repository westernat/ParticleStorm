package org.mesdag.particlestorm.particle;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class ExtendMutableSpriteSet extends ParticleEngine.MutableSpriteSet {
    public TextureAtlasSprite get(int index) {
        return sprites.get(index);
    }
}

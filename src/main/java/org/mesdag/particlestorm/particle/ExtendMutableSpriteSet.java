package org.mesdag.particlestorm.particle;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.ArrayList;
import java.util.List;

public class ExtendMutableSpriteSet extends ParticleEngine.MutableSpriteSet {
    private final List<TextureAtlasSprite> extendedSprites = new ArrayList<>();
    private TextureAtlasSprite missing;

    public TextureAtlasSprite get(int index) {
        if (index < 0 || index >= extendedSprites.size()) return missing;
        return extendedSprites.get(index);
    }

    public void addSprite(TextureAtlasSprite sprite) {
        extendedSprites.add(sprite);
    }

    public void clear() {
        extendedSprites.clear();
    }

    @Override
    public void rebind(List<TextureAtlasSprite> sprites) {
        super.rebind(sprites);
        this.extendedSprites.clear();
        this.extendedSprites.addAll(sprites);
    }

    public void bindMissing(TextureAtlasSprite missing) {
        this.missing = missing;
    }
}

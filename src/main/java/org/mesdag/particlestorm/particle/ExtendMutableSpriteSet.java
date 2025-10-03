package org.mesdag.particlestorm.particle;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ExtendMutableSpriteSet extends ParticleEngine.MutableSpriteSet {
    private TextureAtlasSprite missing;

    public ExtendMutableSpriteSet() {
        this.sprites = new ArrayList<>();
    }

    public TextureAtlasSprite get(int index) {
        if (index < 0 || index >= sprites.size()) return missing;
        return sprites.get(index);
    }

    public void addSprite(TextureAtlasSprite sprite) {
        sprites.add(sprite);
    }

    public void clear() {
        sprites.clear();
    }

    @Override
    public void rebind(@NotNull List<TextureAtlasSprite> sprites) {
        this.sprites = new ArrayList<>(sprites);
    }

    public void bindMissing(TextureAtlasSprite missing) {
        this.missing = missing;
    }
}

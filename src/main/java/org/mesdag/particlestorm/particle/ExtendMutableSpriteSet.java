package org.mesdag.particlestorm.particle;

import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public class ExtendMutableSpriteSet implements SpriteSet {
    private final List<TextureAtlasSprite> sprites = new ArrayList<>();
    private TextureAtlasSprite missing;

    public TextureAtlasSprite get(int index) {
        if (index < 0 || index >= sprites.size()) {
            return missing;
        }
        return sprites.get(index);
    }

    public void addSprite(TextureAtlasSprite sprite) {
        sprites.add(sprite);
    }

    public void clear() {
        sprites.clear();
    }

    public void bindMissing(TextureAtlasSprite missing) {
        this.missing = missing;
    }

    @Override
    public TextureAtlasSprite get(int index, int max) {
        if (sprites.isEmpty()) {
            return missing;
        }
        int resolved = max <= 0 ? 0 : index * (sprites.size() - 1) / max;
        return get(resolved);
    }

    @Override
    public TextureAtlasSprite get(RandomSource random) {
        if (sprites.isEmpty()) {
            return missing;
        }
        return sprites.get(random.nextInt(sprites.size()));
    }

    @Override
    public TextureAtlasSprite first() {
        return sprites.isEmpty() ? missing : sprites.getFirst();
    }
}

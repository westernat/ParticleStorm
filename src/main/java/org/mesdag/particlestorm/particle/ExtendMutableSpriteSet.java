package org.mesdag.particlestorm.particle;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ExtendMutableSpriteSet extends ParticleEngine.MutableSpriteSet {
    private TextureAtlasSprite missing;

    public ExtendMutableSpriteSet() {
        this.sprites = new ArrayList<>();
    }

    public TextureAtlasSprite get(int index) {
        if (index < 0) return missing;
        return sprites.get(index);
    }

    public void addSprite(TextureAtlasSprite sprite) {
        sprites.add(sprite);
    }

    @Override
    public void rebind(@NotNull List<TextureAtlasSprite> sprites) {
        this.sprites = new ArrayList<>(sprites);
    }

    public void setMissing(TextureAtlasSprite missing) {
        this.missing = missing;
    }
}

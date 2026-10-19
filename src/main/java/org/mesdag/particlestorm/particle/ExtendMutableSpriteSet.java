package org.mesdag.particlestorm.particle;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// 按**贴图资源名**索引的 sprite 集合（不再下标记索引：描述里的 `texture` 就是唯一标识）。
///
/// `sprites` 列表只为满足原版 [ParticleEngine.MutableSpriteSet] 的接口（`get(int, int)` / `get(RandomSource)`），
/// 本模组自己的查找走 [#get(ResourceLocation)]。
public class ExtendMutableSpriteSet extends ParticleEngine.MutableSpriteSet {
    private final Map<ResourceLocation, TextureAtlasSprite> byName = new LinkedHashMap<>();
    private TextureAtlasSprite missing;

    public ExtendMutableSpriteSet() {
        this.sprites = new ArrayList<>();
    }

    /// 按贴图资源名取 sprite；没有登记过时返回 missing。
    public TextureAtlasSprite get(ResourceLocation texture) {
        return this.byName.getOrDefault(texture, this.missing);
    }

    /// 登记一个 sprite（同名只保留一个），按名字查找用。
    public void addSprite(TextureAtlasSprite sprite) {
        TextureAtlasSprite previous = this.byName.putIfAbsent(sprite.contents().name(), sprite);
        if (previous == null) {
            this.sprites.add(sprite);
        }
    }

    public void clear() {
        this.sprites.clear();
        this.byName.clear();
    }

    @Override
    public void rebind(List<TextureAtlasSprite> sprites) {
        this.sprites = new ArrayList<>(sprites);
        this.byName.clear();
        for (TextureAtlasSprite sprite : sprites) {
            this.byName.putIfAbsent(sprite.contents().name(), sprite);
        }
    }

    public void bindMissing(TextureAtlasSprite missing) {
        this.missing = missing;
    }

    /// 已登记了多少个 sprite（统计用）。
    public int size() {
        return this.byName.size();
    }

    public @Nullable TextureAtlasSprite find(ResourceLocation texture) {
        return this.byName.get(texture);
    }
}

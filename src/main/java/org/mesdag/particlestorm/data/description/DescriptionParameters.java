package org.mesdag.particlestorm.data.description;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import org.mesdag.particlestorm.PSDiagnostics;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.mixed.ITextureAtlasSprite;

public class DescriptionParameters {
    public static final Identifier MISSING_TEXTURE = ParticleStorm.asResource("missing");
    public static final DescriptionParameters EMPTY = new DescriptionParameters(DescriptionMaterial.CUSTOM, MISSING_TEXTURE);
    public static final Codec<DescriptionParameters> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DescriptionMaterial.CODEC.lenientOptionalFieldOf("material", DescriptionMaterial.CUSTOM).orElse(DescriptionMaterial.PARTICLE_SHEET_TRANSLUCENT).forGetter(DescriptionParameters::material),
            Identifier.CODEC.lenientOptionalFieldOf("texture", MISSING_TEXTURE).forGetter(DescriptionParameters::texture)
    ).apply(instance, DescriptionParameters::new));
    private final DescriptionMaterial material;
    private final Identifier texture;

    public DescriptionParameters(DescriptionMaterial material, Identifier texture) {
        this.material = material;
        this.texture = texture;
    }

    public DescriptionMaterial material() {
        return material;
    }

    public Identifier texture() {
        return texture;
    }

    public TextureAtlasSprite getTexture() {
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES);
        Identifier resolvedTexture = resolveTextureId(atlas, texture);
        TextureAtlasSprite sprite = atlas.getSprite(resolvedTexture);
        boolean missing = MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name());
        PSDiagnostics.infoOnce("texture:" + material + ":" + texture, "texture lookup texture={} resolvedTexture={} material={} spriteName={} missing={} spriteSize={}x{} atlasSize={}x{} u0={} u1={} v0={} v1={}",
                texture,
                resolvedTexture,
                material,
                sprite.contents().name(),
                missing,
                sprite.contents().width(),
                sprite.contents().height(),
                ((ITextureAtlasSprite) sprite).particlestorm$getOriginX(),
                ((ITextureAtlasSprite) sprite).particlestorm$getOriginY(),
                sprite.getU0(),
                sprite.getU1(),
                sprite.getV0(),
                sprite.getV1()
        );
        return sprite;
    }

    private static Identifier resolveTextureId(TextureAtlas atlas, Identifier id) {
        TextureAtlasSprite sprite = atlas.getSprite(id);
        if (!MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name())) {
            return id;
        }
        Identifier fallback = bedrockTextureFallback(id);
        if (fallback.equals(id)) {
            return id;
        }
        TextureAtlasSprite fallbackSprite = atlas.getSprite(fallback);
        if (!MissingTextureAtlasSprite.getLocation().equals(fallbackSprite.contents().name())) {
            PSDiagnostics.infoOnce("texture-fallback:" + id, "texture fallback original={} fallback={}", id, fallback);
            return fallback;
        }
        PSDiagnostics.warnOnce("texture-missing:" + id, "texture missing original={} fallback={}", id, fallback);
        return id;
    }

    private static Identifier bedrockTextureFallback(Identifier id) {
        String path = id.getPath();
        if (path.startsWith("textures/particle/")) {
            path = path.substring("textures/particle/".length());
        } else if (path.startsWith("textures/")) {
            path = path.substring("textures/".length());
        } else {
            return id;
        }
        String namespace = id.getNamespace().equals("minecraft") ? ParticleStorm.MODID : id.getNamespace();
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}

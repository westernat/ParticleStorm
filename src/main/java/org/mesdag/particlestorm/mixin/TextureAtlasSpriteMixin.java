package org.mesdag.particlestorm.mixin;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.mesdag.particlestorm.mixed.ITextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlasSprite.class)
public abstract class TextureAtlasSpriteMixin implements ITextureAtlasSprite {
    @Unique
    private int particlestorm$originX;
    @Unique
    private int particlestorm$originY;

    @Override
    public int particlestorm$getOriginX() {
        return particlestorm$originX;
    }

    @Override
    public int particlestorm$getOriginY() {
        return particlestorm$originY;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void storeOrigin(Identifier atlasLocation, SpriteContents contents, int atlasWidth, int atlasHeight, int x, int y, int padding, CallbackInfo ci) {
        this.particlestorm$originX = atlasWidth;
        this.particlestorm$originY = atlasHeight;
    }
}

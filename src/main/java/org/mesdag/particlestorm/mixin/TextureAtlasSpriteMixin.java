package org.mesdag.particlestorm.mixin;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.ITextureAtlasSprite;
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
    private void storeOrigin(ResourceLocation atlasLocation, SpriteContents contents, int originX, int originY, int x, int y, CallbackInfo ci) {
        this.particlestorm$originX = originX;
        this.particlestorm$originY = originY;
    }
}

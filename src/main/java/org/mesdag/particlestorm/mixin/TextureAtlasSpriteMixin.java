package org.mesdag.particlestorm.mixin;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.mixed.IPSTextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlasSprite.class)
public abstract class TextureAtlasSpriteMixin implements IPSTextureAtlasSprite {
    @Unique
    private float particlestorm$invOx;
    @Unique
    private float particlestorm$invOy;

    @Override
    public float particlestorm$getInvOx() {
        return particlestorm$invOx;
    }

    @Override
    public float particlestorm$getInvOy() {
        return particlestorm$invOy;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void storeOrigin(ResourceLocation atlasLocation, SpriteContents contents, int originX, int originY, int x, int y, CallbackInfo ci) {
        this.particlestorm$invOx = 1.0F / originX;
        this.particlestorm$invOy = 1.0F / originY;
    }
}

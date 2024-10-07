package org.mesdag.particlestorm.mixin;

import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.mesdag.particlestorm.mixinauxi.ITextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(TextureAtlas.class)
public abstract class TextureAtlasMixin implements ITextureAtlas {
    @Unique
    private SpriteLoader.Preparations particlestorm$preparations;

    @Override
    public void particlestorm$consume(Consumer<SpriteLoader.Preparations> consumer) {
        if (particlestorm$preparations != null) {
            consumer.accept(particlestorm$preparations);
            this.particlestorm$preparations = null;
        }
    }

    @Inject(method = "upload", at = @At("HEAD"))
    private void storePreparations(SpriteLoader.Preparations preparations, CallbackInfo ci) {
        this.particlestorm$preparations = preparations;
    }
}

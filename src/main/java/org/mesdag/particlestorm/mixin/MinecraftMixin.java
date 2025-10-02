package org.mesdag.particlestorm.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.mixed.ITextureAtlas;
import org.mesdag.particlestorm.particle.ExtendMutableSpriteSet;
import org.mesdag.particlestorm.particle.MolangParticleInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    @Final
    public ParticleEngine particleEngine;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/ClientHooks;onRegisterParticleProviders(Lnet/minecraft/client/particle/ParticleEngine;)V", remap = false))
    private void registerCustom(GameConfig gameConfig, CallbackInfo ci) {
        ExtendMutableSpriteSet extendMutableSpriteSet = new ExtendMutableSpriteSet();
        ((ParticleEngineAccessor) particleEngine).spriteSets().put(ParticleStorm.MOLANG.getId(), extendMutableSpriteSet);
        ((ParticleEngineAccessor) particleEngine).providers().put(ParticleStorm.MOLANG.getId(), new MolangParticleInstance.Provider(extendMutableSpriteSet));
    }

    @Inject(method = "onResourceLoadFinished", at = @At("TAIL"))
    private void onLoaded(@Coerce Object gameLoadCookie, CallbackInfo ci) {
        if (((ParticleEngineAccessor) particleEngine).spriteSets().get(ParticleStorm.MOLANG.getId()) instanceof ExtendMutableSpriteSet spriteSet) {
            try (TextureAtlas textureAtlas = ((ParticleEngineAccessor) particleEngine).textureAtlas()) {
                ((ITextureAtlas) textureAtlas).particlestorm$consume(preparations -> {
                    spriteSet.clear();
                    int i = 0;
                    for (Map.Entry<ResourceLocation, DefinedParticleEffect> entry : PSGameClient.LOADER.id2Effect().entrySet()) {
                        TextureAtlasSprite missing = preparations.missing();
                        spriteSet.bindMissing(missing);
                        ResourceLocation texture = entry.getValue().description.parameters().bindTexture(i);
                        TextureAtlasSprite sprite = preparations.regions().get(texture);
                        spriteSet.addSprite(sprite == null ? missing : sprite);
                        i++;
                    }
                });
            }
        }
    }
}

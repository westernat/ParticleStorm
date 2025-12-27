package org.mesdag.particlestorm.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.RegisterCustomParticleTypeEvent;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.mixed.IParticleEngine;
import org.mesdag.particlestorm.particle.ExtendMutableSpriteSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin implements IParticleEngine {
    @Shadow
    @Final
    private Map<ResourceLocation, ParticleEngine.MutableSpriteSet> spriteSets;

    @Unique
    private volatile CompletableFuture<SpriteLoader.Preparations> particlestorm$preparations;

    @Override
    public void particlestorm$bindSprites() {
        if (particlestorm$preparations != null && spriteSets.get(ParticleStorm.MOLANG.getId()) instanceof ExtendMutableSpriteSet spriteSet) {
            SpriteLoader.Preparations preparations = particlestorm$preparations.join();
            spriteSet.clear();
            int i = 0;
            for (Map.Entry<ResourceLocation, DefinedParticleEffect> entry : PSGameClient.LOADER.id2Effect().entrySet()) {
                TextureAtlasSprite missing = preparations.missing();
                spriteSet.bindMissing(missing);
                ResourceLocation texture = entry.getValue().description.parameters().bindTexture(i);
                spriteSet.addSprite(preparations.regions().getOrDefault(texture, missing));
                i++;
            }
        }
        this.particlestorm$preparations = null;
    }

    @Inject(method = "registerProviders", at = @At("TAIL"))
    private void registerCustom(CallbackInfo ci) {
        RegisterCustomParticleTypeEvent.postEvent(spriteSets);
    }

    @ModifyReturnValue(method = "reload", at = @At("RETURN"))
    private CompletableFuture<Void> cachePreparations(CompletableFuture<Void> original, @Local(name = "completablefuture1") CompletableFuture<SpriteLoader.Preparations> future) {
        this.particlestorm$preparations = future;
        return original;
    }
}

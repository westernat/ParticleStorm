package org.mesdag.particlestorm.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.RegisterCustomParticleTypeEvent;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.mixed.IPSParticleEngine;
import org.mesdag.particlestorm.particle.ExtendMutableSpriteSet;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin implements IPSParticleEngine {
    @Shadow
    @Final
    private Map<ResourceLocation, ParticleEngine.MutableSpriteSet> spriteSets;

    @Unique
    private volatile SpriteLoader.Preparations particlestorm$preparations;

    @Override
    public void particlestorm$bindSprites() {
        if (particlestorm$preparations != null && spriteSets.get(ParticleStorm.MOLANG.getId()) instanceof ExtendMutableSpriteSet spriteSet) {
            spriteSet.clear();
            int i = 0;
            TextureAtlasSprite missing = particlestorm$preparations.missing();
            spriteSet.bindMissing(missing);
            for (Map.Entry<ResourceLocation, DefinedParticleEffect> entry : MolangParticleEngine.INSTANCE.id2Effect().entrySet()) {
                ResourceLocation texture = entry.getValue().description.parameters().bindTexture(i);
                spriteSet.addSprite(particlestorm$preparations.regions().getOrDefault(texture, missing));
                i++;
            }
        }
        this.particlestorm$preparations = null;
    }

    @Inject(method = "registerProviders", at = @At("TAIL"))
    private void registerCustom(CallbackInfo ci) {
        RegisterCustomParticleTypeEvent.postEvent(spriteSets);
    }

    // patched on build gradle
    @Inject(method = "lambda$reload$9", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureAtlas;upload(Lnet/minecraft/client/renderer/texture/SpriteLoader$Preparations;)V", remap = true), remap = false)
    private void cachePreparations(CallbackInfo ci, @Local SpriteLoader.Preparations spriteloader$preparations) {
        this.particlestorm$preparations = spriteloader$preparations;
    }
}

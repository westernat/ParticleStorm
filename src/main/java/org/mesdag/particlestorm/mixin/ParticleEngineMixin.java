package org.mesdag.particlestorm.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.RegisterCustomParticleTypeEvent;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.mixed.IParticleEngine;
import org.mesdag.particlestorm.particle.ExtendMutableSpriteSet;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.Predicate;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin implements IParticleEngine {
    @Shadow
    @Final
    private Map<ResourceLocation, ParticleEngine.MutableSpriteSet> spriteSets;

    @Shadow
    @Final
    private TextureManager textureManager;
    @Unique
    private volatile SpriteLoader.Preparations particlestorm$preparations;

    @Override
    public void particlestorm$bindSprites() {
        if (particlestorm$preparations != null && spriteSets.get(ParticleStorm.MOLANG.getId()) instanceof ExtendMutableSpriteSet spriteSet) {
            spriteSet.clear();
            int i = 0;
            for (Map.Entry<ResourceLocation, DefinedParticleEffect> entry : MolangParticleEngine.INSTANCE.id2Effect().entrySet()) {
                TextureAtlasSprite missing = particlestorm$preparations.missing();
                spriteSet.bindMissing(missing);
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

    @ModifyArg(method = "lambda$reload$9", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureAtlas;upload(Lnet/minecraft/client/renderer/texture/SpriteLoader$Preparations;)V"))
    private SpriteLoader.Preparations cachePreparations(SpriteLoader.Preparations preparations) {
        return this.particlestorm$preparations = preparations;
    }

    @Inject(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;depthMask(Z)V"))
    private void renderMolang(
            CallbackInfo ci,
            @Local(argsOnly = true) Camera camera,
            @Local(argsOnly = true) float partialTick,
            @Local(argsOnly = true) @Nullable Frustum frustum,
            @Local(argsOnly = true) @Nullable Predicate<ParticleRenderType> renderTypePredicate
    ) {
        if (frustum == null || renderTypePredicate == null) return;
        MolangParticleEngine.INSTANCE.renderParticles(textureManager, camera, partialTick, frustum, renderTypePredicate);
    }
}

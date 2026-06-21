package org.mesdag.particlestorm.mixin.integration.geckolib;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationProcessor;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.model.GeoModel", remap = false)
public abstract class GeoModelMixin<T extends GeoAnimatable> {
    @Unique
    private static final PoseStack particlestorm$postStack = new PoseStack();

    @Inject(method = "handleAnimations", at = @At("TAIL"))
    private void transform(
            CallbackInfo ci,
            @Local(argsOnly = true) T animatable,
            @Local(name = "processor") AnimationProcessor<T> processor
    ) {
        float partialTick = Minecraft.getInstance().getPartialTick();
        for (CoreGeoBone bone : processor.getRegisteredBones()) {
            if (bone instanceof GeoBone geoBone) {
                GeckoLibHelper.transformLocator(particlestorm$postStack, geoBone, animatable, partialTick);
            }
        }
    }
}

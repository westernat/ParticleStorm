package org.mesdag.particlestorm.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.api.MolangParticleMobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @WrapWithCondition(method = "tickEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private boolean modify(Level instance, ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        LivingEntity entity = (LivingEntity) (Object) this;
        List<MobEffectInstance> visibleEffects = entity.getActiveEffects().stream().filter(MobEffectInstance::isVisible).toList();
        if (!visibleEffects.isEmpty() && visibleEffects.get(entity.getRandom().nextInt(visibleEffects.size())).getEffect() instanceof MolangParticleMobEffect effect) {
            PSGameClient.LOADER.addTrackedEmitter(entity, effect.getParticleId());
            return false;
        }
        return true;
    }
}

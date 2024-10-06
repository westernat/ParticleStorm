package org.mesdag.particlestorm.mixin;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.commands.ParticleCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.mesdag.particlestorm.data.event.ParticleEffect;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.MolangParticleOption;
import org.mesdag.particlestorm.particle.ParticleEmitterEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(ParticleCommand.class)
public abstract class ParticleCommandMixin {
    @Inject(method = "sendParticles", at = @At("HEAD"), cancellable = true)
    private static void createEmitter(CommandSourceStack source, ParticleOptions particleData, Vec3 pos, Vec3 delta, float speed, int count, boolean force, Collection<ServerPlayer> viewers, CallbackInfoReturnable<Integer> cir) {
        if (particleData instanceof MolangParticleOption option) {
            ParticleEmitterEntity.ManualData manualData = new ParticleEmitterEntity.ManualData(source.getLevel(), option, pos, delta, speed, count, force, viewers);
            source.getLevel().addFreshEntity(new ParticleEmitterEntity(source.getLevel(), manualData, option.getId(), pos, ParticleEffect.Type.EMITTER, MolangExp.EMPTY));
            cir.setReturnValue(1);
        }
    }
}

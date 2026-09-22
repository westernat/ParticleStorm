package org.mesdag.particlestorm.mixin;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.mesdag.particlestorm.particle.MolangParticleInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(QuadParticleGroup.class)
public abstract class QuadParticleGroupMixin extends ParticleGroup<SingleQuadParticle> {
    @Shadow
    @Final
    private ParticleRenderType particleType;

    @Shadow
    @Final
    private QuadParticleRenderState particleTypeRenderState;

    protected QuadParticleGroupMixin(ParticleEngine engine) {
        super(engine);
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void particlestorm$extractLocalSpaceRenderState(Frustum frustum, Camera camera, float partialTickTime, CallbackInfoReturnable<ParticleGroupRenderState> cir) {
        for (SingleQuadParticle particle : this.particles) {
            if (particlestorm$isVisible(frustum, particle, partialTickTime)) {
                try {
                    particle.extract(this.particleTypeRenderState, camera, partialTickTime);
                } catch (Throwable throwable) {
                    CrashReport report = CrashReport.forThrowable(throwable, "Rendering Particle");
                    CrashReportCategory category = report.addCategory("Particle being rendered");
                    category.setDetail("Particle", particle::toString);
                    category.setDetail("Particle Type", this.particleType::toString);
                    throw new ReportedException(report);
                }
            }
        }

        cir.setReturnValue(this.particleTypeRenderState);
    }

    private static boolean particlestorm$isVisible(Frustum frustum, SingleQuadParticle particle, float partialTickTime) {
        if (particle instanceof MolangParticleInstance molang) {
            return molang.particlestorm$isVisible(frustum, partialTickTime);
        }

        ParticleAccessor accessor = (ParticleAccessor) particle;
        return frustum.pointInFrustum(accessor.particlestorm$getX(), accessor.particlestorm$getY(), accessor.particlestorm$getZ());
    }
}

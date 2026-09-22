package org.mesdag.particlestorm.mixin.integration.geckolib;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTimeline;
import com.geckolib.animation.state.KeyFrameEvent;
import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.animation.keyframeevent.CustomInstructionKeyframeData;
import com.geckolib.cache.animation.keyframeevent.ParticleKeyframeData;
import com.geckolib.cache.animation.keyframeevent.SoundKeyframeData;
import com.geckolib.renderer.base.GeoRenderState;
import org.mesdag.particlestorm.PSDiagnostics;
import org.mesdag.particlestorm.api.geckolib.GeckoLibHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Pseudo
@Mixin(targets = "com.geckolib.animation.state.AnimationTimeline", remap = false)
public abstract class AnimationTimelineMixin {
    @Inject(method = "triggerKeyframeMarkersBetween", at = @At("HEAD"))
    private <T extends GeoAnimatable> void particlestorm$triggerParticleStormKeyframes(
            T animatable,
            GeoRenderState renderState,
            double fromTime,
            double toTime,
            AnimationController<T> controller,
            AnimationController.KeyframeEventHandler<T, SoundKeyframeData> soundHandler,
            AnimationController.KeyframeEventHandler<T, ParticleKeyframeData> particleHandler,
            AnimationController.KeyframeEventHandler<T, CustomInstructionKeyframeData> customInstructionHandler,
            CallbackInfo ci
    ) {
        List<ParticleKeyframeData> particleMarkers = particlestorm$getParticleMarkers(fromTime, toTime);
        if (particleMarkers.isEmpty()) {
            return;
        }

        for (ParticleKeyframeData particleData : toTime < fromTime ? particleMarkers.reversed() : particleMarkers) {
            PSDiagnostics.infoFirstN("geckolib-particle-keyframe-dispatch", 20, "GeckoLib particle keyframe dispatch effect={} locator={} fromTime={} toTime={}",
                    particleData.getEffect(),
                    particleData.getLocatorName(),
                    fromTime,
                    toTime
            );
            GeckoLibHelper.processParticleEffect(new KeyFrameEvent<>(animatable, renderState, controller, particleData));
        }
    }

    @Unique
    private List<ParticleKeyframeData> particlestorm$getParticleMarkers(double fromTime, double toTime) {
        List<ParticleKeyframeData> particleMarkers = new ArrayList<>();
        double minTime = Math.min(fromTime, toTime);
        double maxTime = Math.max(fromTime, toTime);

        for (AnimationTimeline.Stage stage : ((AnimationTimeline) (Object) this).stages()) {
            if (stage.startTime() > maxTime) {
                break;
            }
            if (stage.endTime() <= minTime || stage.isTransition()) {
                continue;
            }

            Animation animation = stage.animation();
            if (animation == null) {
                continue;
            }

            double animationFromTime = Math.max(0, minTime - stage.startTime());
            double animationToTime = Math.min(animation.length(), maxTime - stage.startTime());
            particlestorm$appendParticleMarkers(animationFromTime, animationToTime, animation.keyframeMarkers().particles(), particleMarkers);
        }

        return particleMarkers;
    }

    @Unique
    private static void particlestorm$appendParticleMarkers(double startTime, double endTime, ParticleKeyframeData[] markers, List<ParticleKeyframeData> particleMarkers) {
        for (ParticleKeyframeData marker : markers) {
            if (marker.getTime() > endTime) {
                break;
            }
            if (marker.getTime() > startTime || startTime == 0) {
                particleMarkers.add(marker);
            }
        }
    }
}

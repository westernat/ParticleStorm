package org.mesdag.particlestorm.editor;

import org.mesdag.particlestorm.api.RegisterComponentGridEvent;
import org.mesdag.particlestorm.data.component.*;

import java.util.List;

/// 内置组件的编辑 Grid 注册：id 直接用组件类自己的 `ID` 常量（与 `PSGameClient#registerComponents` 一致）。
///
/// 同族多子类型的组件（rate / lifetime / shape）注册为一个族：一个 Grid + 「类型」下拉切换子类型；
/// `required` 的族（rate / lifetime / shape）在页签里不可删除。
/// 自定义组件可以在模组总线上监听 [RegisterComponentGridEvent] 补自己的 Grid。
public final class ComponentGrids {
    private ComponentGrids() {
    }

    public static void registerBuiltins(RegisterComponentGridEvent event) {
        // 发射器组件
        event.register(EmitterRate.Instant.ID, "emitter_rate",
                List.of(EmitterRate.Instant.ID, EmitterRate.Steady.ID, EmitterRate.Manual.ID), true, EmitterRateGrid::new);
        event.register(EmitterLifetime.Looping.ID, "emitter_lifetime",
                List.of(EmitterLifetime.Looping.ID, EmitterLifetime.Once.ID, EmitterLifetime.Expression.ID), true, EmitterLifetimeGrid::new);
        event.register(EmitterShape.Point.ID, "emitter_shape",
                List.of(EmitterShape.Point.ID, EmitterShape.Sphere.ID, EmitterShape.Box.ID, EmitterShape.Disc.ID, EmitterShape.EntityAABB.ID),
                true, EmitterShapeGrid::new);
        event.register(EmitterLocalSpace.ID, EmitterLocalSpaceGrid::new);
        event.register(EmitterInitialization.ID, EmitterInitializationGrid::new);
        event.register(EmitterLifetimeEvents.ID, EmitterLifetimeEventsGrid::new);
        event.register(EmitterMultiShape.ID, EmitterMultiShapeGrid::new);

        // 粒子组件
        event.register(ParticleInitialSpeed.ID, ParticleInitialSpeedGrid::new);
        event.register(ParticleInitialSpin.ID, ParticleInitialSpinGrid::new);
        event.register(ParticleInitialization.ID, ParticleInitializationGrid::new);
        event.register(ParticleMotionDynamic.ID, ParticleMotionDynamicGrid::new);
        event.register(ParticleMotionParametric.ID, ParticleMotionParametricGrid::new);
        event.register(ParticleLifetimeExpression.ID, ParticleLifetimeExpressionGrid::new);
        event.register(ParticleLifetimeKillPlane.ID, ParticleKillPlaneGrid::new);
        event.register(ParticleExpireIfInBlocks.ID, ParticleExpireIfInBlocksGrid::new);
        event.register(ParticleExpireIfNotInBlocks.ID, ParticleExpireIfNotInBlocksGrid::new);
        event.register(ParticleAppearanceBillboard.ID, ParticleAppearanceBillboardGrid::new);
        event.register(ParticleAppearanceLighting.ID, ParticleAppearanceLightingGrid::new);
        event.register(ParticleAppearanceTinting.ID, ParticleAppearanceTintingGrid::new);
        event.register(ParticleLifeTimeEvents.ID, ParticleLifeTimeEventsGrid::new);
        event.register(ParticleMotionCollision.ID, ParticleMotionCollisionGrid::new);
    }
}

package org.mesdag.particlestorm.editor;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.Font;
import org.mesdag.particlestorm.api.IComponent;
import org.mesdag.particlestorm.data.component.*;
import org.mesdag.particlestorm.data.component.ParticleAppearanceBillboard.Direction;
import org.mesdag.particlestorm.data.component.ParticleAppearanceBillboard.FaceCameraMode;
import org.mesdag.particlestorm.data.component.ParticleAppearanceBillboard.UV;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.FloatMolangExp2;
import org.mesdag.particlestorm.data.molang.FloatMolangExp3;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/// 粒子侧组件的编辑 Grid（`minecraft:particle_*`）。
final class ParticleInitialSpeedGrid extends ComponentGrid {
    private static final List<String> MODES = List.of("scalar", "vector");
    private int mode;
    private FloatMolangExp scalar = FloatMolangExp.ONE;
    private FloatMolangExp3 vector = FloatMolangExp3.X;

    ParticleInitialSpeedGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.setComponentId(ParticleInitialSpeed.ID);
        this.addModeRow();
        this.addValueRows();
    }

    @Override
    public BuiltComponent createDefault() {
        return new BuiltComponent(ParticleInitialSpeed.ID, new ParticleInitialSpeed(Either.left(FloatMolangExp.ONE)));
    }

    @Override
    public BuiltComponent build() {
        return new BuiltComponent(ParticleInitialSpeed.ID,
                new ParticleInitialSpeed(this.mode == 0 ? Either.left(this.scalar) : Either.right(this.vector)));
    }

    @Override
    public void load(IComponent component) {
        ParticleInitialSpeed speed = (ParticleInitialSpeed) component;
        speed.speed().ifLeft(exp -> {
            this.mode = 0;
            this.scalar = exp;
        }).ifRight(exp3 -> {
            this.mode = 1;
            this.vector = exp3;
        });
        this.rebuildRows();
    }

    private void addModeRow() {
        this.select("mode", localizedOptions("particle_initial_speed",
                MODES.stream().map(value -> "mode_" + value).toList()), this.mode, index -> {
            this.mode = index;
            this.rebuildRows();
            this.markChanged();
        });
    }

    private void addValueRows() {
        if (this.mode == 0) {
            this.text("speed", expText(this.scalar), text -> {
                this.scalar = expOf(text);
                this.markChanged();
            });
        } else {
            this.vector("speed", this.vector, value -> {
                this.vector = value;
                this.markChanged();
            });
        }
    }

    private void rebuildRows() {
        this.resetRows();
        this.addModeRow();
        this.addValueRows();
    }
}

final class ParticleInitialSpinGrid extends ComponentGrid {
    private FloatMolangExp rotation = FloatMolangExp.ZERO;
    private FloatMolangExp rotationRate = FloatMolangExp.ZERO;
    private final TextRow rotationRow;
    private final TextRow rotationRateRow;

    ParticleInitialSpinGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.setComponentId(ParticleInitialSpin.ID);
        this.rotationRow = this.text("rotation", expText(this.rotation), text -> {
            this.rotation = expOf(text);
            this.markChanged();
        });
        this.rotationRateRow = this.text("rotation_rate", expText(this.rotationRate), text -> {
            this.rotationRate = expOf(text);
            this.markChanged();
        });
    }

    @Override
    public BuiltComponent createDefault() {
        return new BuiltComponent(ParticleInitialSpin.ID, new ParticleInitialSpin(FloatMolangExp.ZERO, FloatMolangExp.ZERO));
    }

    @Override
    public BuiltComponent build() {
        return new BuiltComponent(ParticleInitialSpin.ID, new ParticleInitialSpin(this.rotation, this.rotationRate));
    }

    @Override
    public void load(IComponent component) {
        ParticleInitialSpin spin = (ParticleInitialSpin) component;
        this.rotation = spin.rotation();
        this.rotationRate = spin.rotationRate();
        this.rotationRow.setValue(expText(this.rotation));
        this.rotationRateRow.setValue(expText(this.rotationRate));
    }
}

final class ParticleInitializationGrid extends ComponentGrid {
    private FloatMolangExp perRender = FloatMolangExp.ZERO;
    private FloatMolangExp perUpdate = FloatMolangExp.ZERO;
    private final TextRow perRenderRow;
    private final TextRow perUpdateRow;

    ParticleInitializationGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.setComponentId(ParticleInitialization.ID);
        this.perRenderRow = this.text("per_render_expression", expText(this.perRender), text -> {
            this.perRender = expOf(text);
            this.markChanged();
        });
        this.perUpdateRow = this.text("per_update_expression", expText(this.perUpdate), text -> {
            this.perUpdate = expOf(text);
            this.markChanged();
        });
    }

    @Override
    public BuiltComponent createDefault() {
        return new BuiltComponent(ParticleInitialization.ID, new ParticleInitialization(FloatMolangExp.ZERO, FloatMolangExp.ZERO));
    }

    @Override
    public BuiltComponent build() {
        return new BuiltComponent(ParticleInitialization.ID, new ParticleInitialization(this.perRender, this.perUpdate));
    }

    @Override
    public void load(IComponent component) {
        ParticleInitialization initialization = (ParticleInitialization) component;
        this.perRender = initialization.perRenderExpression();
        this.perUpdate = initialization.perUpdateExpression();
        this.perRenderRow.setValue(expText(this.perRender));
        this.perUpdateRow.setValue(expText(this.perUpdate));
    }
}

final class ParticleMotionDynamicGrid extends ComponentGrid {
    private FloatMolangExp3 linearAcceleration = FloatMolangExp3.ZERO;
    private FloatMolangExp linearDrag = FloatMolangExp.ZERO;
    private FloatMolangExp rotationAcceleration = FloatMolangExp.ZERO;
    private FloatMolangExp rotationDrag = FloatMolangExp.ZERO;
    private final VectorRow linearAccelerationRow;
    private final TextRow linearDragRow;
    private final TextRow rotationAccelerationRow;
    private final TextRow rotationDragRow;

    ParticleMotionDynamicGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.setComponentId(ParticleMotionDynamic.ID);
        this.linearAccelerationRow = this.vector("linear_acceleration", this.linearAcceleration, value -> {
            this.linearAcceleration = value;
            this.markChanged();
        });
        this.linearDragRow = this.text("linear_drag_coefficient", expText(this.linearDrag), text -> {
            this.linearDrag = expOf(text);
            this.markChanged();
        });
        this.rotationAccelerationRow = this.text("rotation_acceleration", expText(this.rotationAcceleration), text -> {
            this.rotationAcceleration = expOf(text);
            this.markChanged();
        });
        this.rotationDragRow = this.text("rotation_drag_coefficient", expText(this.rotationDrag), text -> {
            this.rotationDrag = expOf(text);
            this.markChanged();
        });
    }

    @Override
    public BuiltComponent createDefault() {
        return new BuiltComponent(ParticleMotionDynamic.ID,
                new ParticleMotionDynamic(FloatMolangExp3.ZERO, FloatMolangExp.ZERO, FloatMolangExp.ZERO, FloatMolangExp.ZERO));
    }

    @Override
    public BuiltComponent build() {
        return new BuiltComponent(ParticleMotionDynamic.ID,
                new ParticleMotionDynamic(this.linearAcceleration, this.linearDrag, this.rotationAcceleration, this.rotationDrag));
    }

    @Override
    public void load(IComponent component) {
        ParticleMotionDynamic motion = (ParticleMotionDynamic) component;
        this.linearAcceleration = motion.linerAcceleration();
        this.linearDrag = motion.linearDragCoefficient();
        this.rotationAcceleration = motion.rotationAcceleration();
        this.rotationDrag = motion.rotationDragCoefficient();
        this.linearAccelerationRow.setValue(this.linearAcceleration);
        this.linearDragRow.setValue(expText(this.linearDrag));
        this.rotationAccelerationRow.setValue(expText(this.rotationAcceleration));
        this.rotationDragRow.setValue(expText(this.rotationDrag));
    }
}

final class ParticleMotionParametricGrid extends ComponentGrid {
    private FloatMolangExp3 relativePosition = FloatMolangExp3.ZERO;
    private FloatMolangExp3 direction = FloatMolangExp3.ZERO;
    private FloatMolangExp rotation = FloatMolangExp.ZERO;
    private final VectorRow relativePositionRow;
    private final VectorRow directionRow;
    private final TextRow rotationRow;

    ParticleMotionParametricGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.setComponentId(ParticleMotionParametric.ID);
        this.relativePositionRow = this.vector("relative_position", this.relativePosition, value -> {
            this.relativePosition = value;
            this.markChanged();
        });
        this.directionRow = this.vector("direction", this.direction, value -> {
            this.direction = value;
            this.markChanged();
        });
        this.rotationRow = this.text("rotation", expText(this.rotation), text -> {
            this.rotation = expOf(text);
            this.markChanged();
        });
    }

    @Override
    public BuiltComponent createDefault() {
        return new BuiltComponent(ParticleMotionParametric.ID,
                new ParticleMotionParametric(FloatMolangExp3.ZERO, FloatMolangExp3.ZERO, FloatMolangExp.ZERO));
    }

    @Override
    public BuiltComponent build() {
        return new BuiltComponent(ParticleMotionParametric.ID,
                new ParticleMotionParametric(this.relativePosition, this.direction, this.rotation));
    }

    @Override
    public void load(IComponent component) {
        ParticleMotionParametric parametric = (ParticleMotionParametric) component;
        this.relativePosition = parametric.relativePosition();
        this.direction = parametric.direction();
        this.rotation = parametric.rotation();
        this.relativePositionRow.setValue(this.relativePosition);
        this.directionRow.setValue(this.direction);
        this.rotationRow.setValue(expText(this.rotation));
    }
}

final class ParticleLifetimeExpressionGrid extends ComponentGrid {
    private FloatMolangExp expiration = FloatMolangExp.ZERO;
    private FloatMolangExp maxLifetime = FloatMolangExp.ZERO;
    private final TextRow expirationRow;
    private final TextRow maxLifetimeRow;

    ParticleLifetimeExpressionGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.setComponentId(ParticleLifetimeExpression.ID);
        this.expirationRow = this.text("expiration_expression", expText(this.expiration), text -> {
            this.expiration = expOf(text);
            this.markChanged();
        });
        this.maxLifetimeRow = this.text("max_lifetime", expText(this.maxLifetime), text -> {
            this.maxLifetime = expOf(text);
            this.markChanged();
        });
    }

    @Override
    public BuiltComponent createDefault() {
        return new BuiltComponent(ParticleLifetimeExpression.ID,
                new ParticleLifetimeExpression(FloatMolangExp.ZERO, FloatMolangExp.ZERO));
    }

    @Override
    public BuiltComponent build() {
        return new BuiltComponent(ParticleLifetimeExpression.ID,
                new ParticleLifetimeExpression(this.expiration, this.maxLifetime));
    }

    @Override
    public void load(IComponent component) {
        ParticleLifetimeExpression expression = (ParticleLifetimeExpression) component;
        this.expiration = expression.expirationExpression();
        this.maxLifetime = expression.maxLifetime();
        this.expirationRow.setValue(expText(this.expiration));
        this.maxLifetimeRow.setValue(expText(this.maxLifetime));
    }
}

final class ParticleKillPlaneGrid extends ComponentGrid {
    private float a;
    private float b;
    private float c;
    private float d;
    private final TextRow aRow;
    private final TextRow bRow;
    private final TextRow cRow;
    private final TextRow dRow;

    ParticleKillPlaneGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.setComponentId(ParticleLifetimeKillPlane.ID);
        this.aRow = this.text("a", String.valueOf(this.a), text -> {
            this.a = floatOf(text);
            this.markChanged();
        });
        this.bRow = this.text("b", String.valueOf(this.b), text -> {
            this.b = floatOf(text);
            this.markChanged();
        });
        this.cRow = this.text("c", String.valueOf(this.c), text -> {
            this.c = floatOf(text);
            this.markChanged();
        });
        this.dRow = this.text("d", String.valueOf(this.d), text -> {
            this.d = floatOf(text);
            this.markChanged();
        });
    }

    @Override
    public BuiltComponent createDefault() {
        return new BuiltComponent(ParticleLifetimeKillPlane.ID, new ParticleLifetimeKillPlane(0, 0, 0, 0));
    }

    @Override
    public BuiltComponent build() {
        return new BuiltComponent(ParticleLifetimeKillPlane.ID, new ParticleLifetimeKillPlane(this.a, this.b, this.c, this.d));
    }

    @Override
    public void load(IComponent component) {
        ParticleLifetimeKillPlane plane = (ParticleLifetimeKillPlane) component;
        this.a = plane.A;
        this.b = plane.B;
        this.c = plane.C;
        this.d = plane.D;
        this.aRow.setValue(String.valueOf(this.a));
        this.bRow.setValue(String.valueOf(this.b));
        this.cRow.setValue(String.valueOf(this.c));
        this.dRow.setValue(String.valueOf(this.d));
    }
}

final class ParticleExpireIfInBlocksGrid extends ComponentGrid {
    private String blocks = "";
    private final TextRow blocksRow;

    ParticleExpireIfInBlocksGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.setComponentId(ParticleExpireIfInBlocks.ID);
        this.blocksRow = this.text("blocks", "", text -> {
            this.blocks = text;
            this.markChanged();
        });
    }

    @Override
    public BuiltComponent createDefault() {
        return new BuiltComponent(ParticleExpireIfInBlocks.ID, new ParticleExpireIfInBlocks(new HashSet<>()));
    }

    @Override
    public BuiltComponent build() {
        return new BuiltComponent(ParticleExpireIfInBlocks.ID, new ParticleExpireIfInBlocks(parseIds(this.blocks)));
    }

    @Override
    public void load(IComponent component) {
        this.blocks = String.join(", ", ((ParticleExpireIfInBlocks) component).ids);
        this.blocksRow.setValue(this.blocks);
    }

    static HashSet<String> parseIds(String text) {
        HashSet<String> ids = new HashSet<>();
        for (String part : text.split(",")) {
            String id = part.trim();
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        return ids;
    }
}

final class ParticleExpireIfNotInBlocksGrid extends ComponentGrid {
    private String blocks = "";
    private final TextRow blocksRow;

    ParticleExpireIfNotInBlocksGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.setComponentId(ParticleExpireIfNotInBlocks.ID);
        this.blocksRow = this.text("blocks", "", text -> {
            this.blocks = text;
            this.markChanged();
        });
    }

    @Override
    public BuiltComponent createDefault() {
        return new BuiltComponent(ParticleExpireIfNotInBlocks.ID, new ParticleExpireIfNotInBlocks(new HashSet<>()));
    }

    @Override
    public BuiltComponent build() {
        return new BuiltComponent(ParticleExpireIfNotInBlocks.ID,
                new ParticleExpireIfNotInBlocks(ParticleExpireIfInBlocksGrid.parseIds(this.blocks)));
    }

    @Override
    public void load(IComponent component) {
        this.blocks = String.join(", ", ((ParticleExpireIfNotInBlocks) component).ids);
        this.blocksRow.setValue(this.blocks);
    }
}

final class ParticleAppearanceBillboardGrid extends ComponentGrid {
    private static final List<String> DIRECTION_MODES = List.of("derive_from_velocity", "custom_direction");
    private static final int DERIVE_FROM_VELOCITY = 0;
    private static final int CUSTOM_DIRECTION = 1;

    private FloatMolangExp2 size = FloatMolangExp2.ZERO;
    private FaceCameraMode faceCameraMode = FaceCameraMode.ROTATE_XYZ;
    private int directionMode = DERIVE_FROM_VELOCITY;
    private float minSpeedThreshold = 0.01F;
    private FloatMolangExp3 customDirection = FloatMolangExp3.ZERO;
    private UV uv = UV.EMPTY;

    ParticleAppearanceBillboardGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.setComponentId(ParticleAppearanceBillboard.ID);
        this.rebuildRows();
    }

    /// 方向模式决定显示哪一组字段：由速度推导 → 最小速度阈值；自定义方向 → 自定义方向向量。
    private void rebuildRows() {
        this.resetRows();
        this.pair("size", this.size, value -> {
            this.size = value;
            this.markChanged();
        });
        this.select("face_camera_mode",
                Arrays.stream(FaceCameraMode.values())
                        .map(mode -> optionLabel("particle_appearance_billboard", "face_camera_mode_" + mode.getSerializedName()))
                        .toList(),
                this.faceCameraMode.ordinal(), index -> {
                    this.faceCameraMode = FaceCameraMode.values()[index];
                    this.markChanged();
                });
        this.select("direction_mode",
                localizedOptions("particle_appearance_billboard",
                        DIRECTION_MODES.stream().map(value -> "direction_mode_" + value).toList()), this.directionMode, index -> {
                    this.directionMode = index;
                    this.rebuildRows();
                    this.markChanged();
                });
        if (this.directionMode == CUSTOM_DIRECTION) {
            this.vector("custom_direction", this.customDirection, value -> {
                this.customDirection = value;
                this.markChanged();
            });
        } else {
            this.text("min_speed_threshold", String.valueOf(this.minSpeedThreshold), text -> {
                this.minSpeedThreshold = floatOf(text);
                this.markChanged();
            });
        }
    }

    @Override
    public BuiltComponent createDefault() {
        return new BuiltComponent(ParticleAppearanceBillboard.ID,
                new ParticleAppearanceBillboard(FloatMolangExp2.ZERO, FaceCameraMode.ROTATE_XYZ, Direction.DEFAULT, UV.EMPTY));
    }

    @Override
    public BuiltComponent build() {
        Direction direction = this.directionMode == CUSTOM_DIRECTION
                ? new Direction(Direction.Mode.CUSTOM_DIRECTION, 0.0F, this.customDirection)
                : new Direction(Direction.Mode.DERIVE_FROM_VELOCITY, this.minSpeedThreshold, FloatMolangExp3.ZERO);
        return new BuiltComponent(ParticleAppearanceBillboard.ID,
                new ParticleAppearanceBillboard(this.size, this.faceCameraMode, direction, this.uv));
    }

    @Override
    public void load(IComponent component) {
        ParticleAppearanceBillboard billboard = (ParticleAppearanceBillboard) component;
        this.size = billboard.size();
        this.faceCameraMode = billboard.faceCameraMode();
        Direction direction = billboard.direction();
        this.directionMode = direction.mode() == Direction.Mode.CUSTOM_DIRECTION ? CUSTOM_DIRECTION : DERIVE_FROM_VELOCITY;
        this.minSpeedThreshold = direction.minSpeedThreshold();
        this.customDirection = direction.customDirection();
        this.uv = billboard.uv();
        this.rebuildRows();
    }
}

final class ParticleAppearanceLightingGrid extends ComponentGrid {
    ParticleAppearanceLightingGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.setComponentId(ParticleAppearanceLighting.ID);
        this.info("fields", "(none)");
    }

    @Override
    public BuiltComponent createDefault() {
        return new BuiltComponent(ParticleAppearanceLighting.ID, ParticleAppearanceLighting.INSTANCE);
    }

    @Override
    public BuiltComponent build() {
        return new BuiltComponent(ParticleAppearanceLighting.ID, ParticleAppearanceLighting.INSTANCE);
    }
}

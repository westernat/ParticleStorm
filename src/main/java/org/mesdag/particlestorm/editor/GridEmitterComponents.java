package org.mesdag.particlestorm.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import org.mesdag.particlestorm.api.IComponent;
import org.mesdag.particlestorm.data.component.*;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.FloatMolangExp3;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.List;

/// 发射器侧组件的编辑 Grid（`minecraft:emitter_*`）。
///
/// 同族多子类型的组件（rate / lifetime / shape）只写一个 Grid，用「类型」下拉决定子类型；
/// `createDefault` / `build` 返回 [BuiltComponent] 把子类型 id 和实例一起带出来。
final class EmitterRateGrid extends ComponentGrid {
    private static final List<ResourceLocation> IDS = List.of(EmitterRate.Instant.ID, EmitterRate.Steady.ID, EmitterRate.Manual.ID);
    private static final int INSTANT = 0;
    private static final int STEADY = 1;
    private static final int MANUAL = 2;

    private int subtype;
    private FloatMolangExp numParticles = FloatMolangExp.ofConstant(10);
    private FloatMolangExp spawnRate = FloatMolangExp.ONE;
    private FloatMolangExp maxParticles = FloatMolangExp.ofConstant(50);

    EmitterRateGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.setComponentId(EmitterRate.Instant.ID);
        this.addSubtypeRow();
        this.addFields();
    }

    @Override
    public BuiltComponent createDefault() {
        return new BuiltComponent(EmitterRate.Instant.ID, new EmitterRate.Instant(FloatMolangExp.ofConstant(10)));
    }

    @Override
    public BuiltComponent build() {
        return switch (this.subtype) {
            case STEADY -> new BuiltComponent(EmitterRate.Steady.ID, new EmitterRate.Steady(this.spawnRate, this.maxParticles));
            case MANUAL -> new BuiltComponent(EmitterRate.Manual.ID, new EmitterRate.Manual(this.maxParticles));
            default -> new BuiltComponent(EmitterRate.Instant.ID, new EmitterRate.Instant(this.numParticles));
        };
    }

    @Override
    public void load(IComponent component) {
        this.subtype = switch (component) {
            case EmitterRate.Steady ignored -> STEADY;
            case EmitterRate.Manual ignored -> MANUAL;
            default -> INSTANT;
        };
        switch (component) {
            case EmitterRate.Instant instant -> this.numParticles = instant.getNumParticles();
            case EmitterRate.Steady steady -> {
                this.spawnRate = steady.getSpawnRate();
                this.maxParticles = steady.getMaxParticles();
            }
            case EmitterRate.Manual manual -> this.maxParticles = manual.getMaxParticles();
            default -> {
            }
        }
        this.setComponentId(switch (this.subtype) {
            case STEADY -> EmitterRate.Steady.ID;
            case MANUAL -> EmitterRate.Manual.ID;
            default -> EmitterRate.Instant.ID;
        });
        this.rebuildRows();
    }

    private void addSubtypeRow() {
        this.select("type", variantOptionLabels(IDS, "emitter_rate"), this.subtype, index -> {
            this.subtype = index;
            this.setComponentId(switch (this.subtype) {
                case STEADY -> EmitterRate.Steady.ID;
                case MANUAL -> EmitterRate.Manual.ID;
                default -> EmitterRate.Instant.ID;
            });
            this.rebuildRows();
            this.markChanged();
        });
    }

    private void rebuildRows() {
        this.resetRows();
        this.addSubtypeRow();
        this.addFields();
    }

    private void addFields() {
        switch (this.subtype) {
            case STEADY -> {
                this.text("spawn_rate", expText(this.spawnRate), text -> {
                    this.spawnRate = expOf(text);
                    this.markChanged();
                });
                this.text("max_particles", expText(this.maxParticles), text -> {
                    this.maxParticles = expOf(text);
                    this.markChanged();
                });
            }
            case MANUAL -> this.text("max_particles", expText(this.maxParticles), text -> {
                this.maxParticles = expOf(text);
                this.markChanged();
            });
            default -> this.text("num_particles", expText(this.numParticles), text -> {
                this.numParticles = expOf(text);
                this.markChanged();
            });
        }
    }
}

final class EmitterLifetimeGrid extends ComponentGrid {
    private static final List<ResourceLocation> IDS = List.of(EmitterLifetime.Looping.ID, EmitterLifetime.Once.ID, EmitterLifetime.Expression.ID);
    private static final int LOOPING = 0;
    private static final int ONCE = 1;
    private static final int EXPRESSION = 2;

    private int subtype;
    private FloatMolangExp activeTime = FloatMolangExp.ofConstant(10);
    private FloatMolangExp sleepTime = FloatMolangExp.ZERO;
    private FloatMolangExp activation = FloatMolangExp.ONE;
    private FloatMolangExp expiration = FloatMolangExp.ZERO;

    EmitterLifetimeGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.setComponentId(EmitterLifetime.Looping.ID);
        this.addSubtypeRow();
        this.addFields();
    }

    @Override
    public BuiltComponent createDefault() {
        return new BuiltComponent(EmitterLifetime.Looping.ID,
                new EmitterLifetime.Looping(FloatMolangExp.ofConstant(10), FloatMolangExp.ZERO));
    }

    @Override
    public BuiltComponent build() {
        return switch (this.subtype) {
            case ONCE -> new BuiltComponent(EmitterLifetime.Once.ID, new EmitterLifetime.Once(this.activeTime));
            case EXPRESSION -> new BuiltComponent(EmitterLifetime.Expression.ID,
                    new EmitterLifetime.Expression(this.activation, this.expiration));
            default -> new BuiltComponent(EmitterLifetime.Looping.ID,
                    new EmitterLifetime.Looping(this.activeTime, this.sleepTime));
        };
    }

    @Override
    public void load(IComponent component) {
        this.subtype = switch (component) {
            case EmitterLifetime.Once ignored -> ONCE;
            case EmitterLifetime.Expression ignored -> EXPRESSION;
            default -> LOOPING;
        };
        switch (component) {
            case EmitterLifetime.Looping looping -> {
                this.activeTime = looping.getActiveTime();
                this.sleepTime = looping.getSleepTime();
            }
            case EmitterLifetime.Once once -> this.activeTime = once.getActiveTime();
            case EmitterLifetime.Expression expression -> {
                this.activation = expression.getActivationExpression();
                this.expiration = expression.getExpirationExpression();
            }
            default -> {
            }
        }
        this.setComponentId(switch (this.subtype) {
            case ONCE -> EmitterLifetime.Once.ID;
            case EXPRESSION -> EmitterLifetime.Expression.ID;
            default -> EmitterLifetime.Looping.ID;
        });
        this.rebuildRows();
    }

    private void addSubtypeRow() {
        this.select("type", variantOptionLabels(IDS, "emitter_lifetime"), this.subtype, index -> {
            this.subtype = index;
            this.setComponentId(switch (this.subtype) {
                case ONCE -> EmitterLifetime.Once.ID;
                case EXPRESSION -> EmitterLifetime.Expression.ID;
                default -> EmitterLifetime.Looping.ID;
            });
            this.rebuildRows();
            this.markChanged();
        });
    }

    private void rebuildRows() {
        this.resetRows();
        this.addSubtypeRow();
        this.addFields();
    }

    private void addFields() {
        switch (this.subtype) {
            case ONCE -> this.text("active_time", expText(this.activeTime), text -> {
                this.activeTime = expOf(text);
                this.markChanged();
            });
            case EXPRESSION -> {
                this.text("activation_expression", expText(this.activation), text -> {
                    this.activation = expOf(text);
                    this.markChanged();
                });
                this.text("expiration_expression", expText(this.expiration), text -> {
                    this.expiration = expOf(text);
                    this.markChanged();
                });
            }
            default -> {
                this.text("active_time", expText(this.activeTime), text -> {
                    this.activeTime = expOf(text);
                    this.markChanged();
                });
                this.text("sleep_time", expText(this.sleepTime), text -> {
                    this.sleepTime = expOf(text);
                    this.markChanged();
                });
            }
        }
    }
}

/// 五种形状共用一个 Grid：上方「类型」下拉决定用哪一种，下方字段随类型切换。
/// 具体字段行由 [ShapeFields] 构造（和 `emitter_multi_shape` 共用同一套形状编辑逻辑）。
final class EmitterShapeGrid extends ComponentGrid {
    private final ShapeFields shape = new ShapeFields(this::rebuildRows, null);

    EmitterShapeGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.rebuildRows();
    }

    private void rebuildRows() {
        this.resetRows();
        this.setComponentId(this.shape.subtypeId());
        this.shape.addRows(this);
    }

    @Override
    public BuiltComponent createDefault() {
        return new BuiltComponent(EmitterShape.Point.ID, new EmitterShape.Point(FloatMolangExp3.ZERO, EmitterShape.Direction.OUTWARDS));
    }

    @Override
    public BuiltComponent build() {
        return new BuiltComponent(this.shape.subtypeId(), this.shape.build());
    }

    @Override
    public void load(IComponent component) {
        if (component instanceof EmitterShape shape) {
            this.shape.load(shape);
        }
        this.rebuildRows();
    }
}

final class EmitterLocalSpaceGrid extends ComponentGrid {
    private boolean position;
    private boolean rotation;
    private boolean velocity;
    private final ToggleRow positionRow;
    private final ToggleRow rotationRow;
    private final ToggleRow velocityRow;

    EmitterLocalSpaceGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.setComponentId(EmitterLocalSpace.ID);
        this.positionRow = this.toggle("position", false, value -> {
            this.position = value;
            this.markChanged();
        });
        this.rotationRow = this.toggle("rotation", false, value -> {
            this.rotation = value;
            this.markChanged();
        });
        this.velocityRow = this.toggle("velocity", false, value -> {
            this.velocity = value;
            this.markChanged();
        });
    }

    @Override
    public BuiltComponent createDefault() {
        return new BuiltComponent(EmitterLocalSpace.ID, new EmitterLocalSpace(false, false, false));
    }

    @Override
    public BuiltComponent build() {
        // rotation = true 且 position = false 时构造器会抛异常，由宿主显示错误
        return new BuiltComponent(EmitterLocalSpace.ID, new EmitterLocalSpace(this.position, this.rotation, this.velocity));
    }

    @Override
    public void load(IComponent component) {
        EmitterLocalSpace localSpace = (EmitterLocalSpace) component;
        this.position = localSpace.position();
        this.rotation = localSpace.rotation();
        this.velocity = localSpace.velocity();
        this.positionRow.setValue(this.position);
        this.rotationRow.setValue(this.rotation);
        this.velocityRow.setValue(this.velocity);
    }
}

final class EmitterInitializationGrid extends ComponentGrid {
    private MolangExp creation = MolangExp.EMPTY;
    private MolangExp perUpdate = MolangExp.EMPTY;
    private final TextRow creationRow;
    private final TextRow perUpdateRow;

    EmitterInitializationGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.setComponentId(EmitterInitialization.ID);
        this.creationRow = this.text("creation_expression", expText(this.creation), text -> {
            this.creation = molangOf(text);
            this.markChanged();
        });
        this.perUpdateRow = this.text("per_update_expression", expText(this.perUpdate), text -> {
            this.perUpdate = molangOf(text);
            this.markChanged();
        });
    }

    @Override
    public BuiltComponent createDefault() {
        return new BuiltComponent(EmitterInitialization.ID, new EmitterInitialization(MolangExp.EMPTY, MolangExp.EMPTY));
    }

    @Override
    public BuiltComponent build() {
        return new BuiltComponent(EmitterInitialization.ID, new EmitterInitialization(this.creation, this.perUpdate));
    }

    @Override
    public void load(IComponent component) {
        EmitterInitialization initialization = (EmitterInitialization) component;
        this.creation = initialization.creationExpression();
        this.perUpdate = initialization.perUpdateExpression();
        this.creationRow.setValue(expText(this.creation));
        this.perUpdateRow.setValue(expText(this.perUpdate));
    }
}

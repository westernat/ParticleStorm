package org.mesdag.particlestorm.editor;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.data.component.EmitterShape;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.FloatMolangExp3;

import java.util.List;
import java.util.function.Supplier;

/// 发射器形状（`EmitterShape` 五个子类型）的编辑状态与行构造。
///
/// 单独的形状 [EmitterShapeGrid] 与 `emitter_multi_shape` 里的多个形状条目共用这套逻辑：
/// - `prefix` 为 null 时行标签走组件字段翻译键（`particlestorm.editor.minecraft.emitter_shape_<子类型>.<字段>`）；
/// - `prefix` 非 null 时行标签是「前缀 + 字段名」的字面量（例如 `shape[0].offset`），用于多条目 Grid；
/// - 子类型 / 方向 / 平面切换时会回调 `structuralChange`（宿主负责重建全部行）。
final class ShapeFields {
    static final List<ResourceLocation> IDS = List.of(EmitterShape.Point.ID, EmitterShape.Sphere.ID,
            EmitterShape.Box.ID, EmitterShape.Disc.ID, EmitterShape.EntityAABB.ID);
    private static final int POINT = 0;
    private static final int SPHERE = 1;
    private static final int BOX = 2;
    private static final int DISC = 3;
    private static final int ENTITY_AABB = 4;
    private static final List<String> DIRECTION_OPTIONS = List.of("inwards", "outwards", "custom");
    private static final List<String> PLANE_OPTIONS = List.of("x", "y", "z", "custom");
    private static final int DIRECTION_OUTWARDS = 1;
    private static final int DIRECTION_CUSTOM = 2;
    private static final int PLANE_Y = 1;
    private static final int PLANE_CUSTOM = 3;

    private final Runnable structuralChange;
    private final @Nullable Supplier<String> prefix;
    private int subtype = POINT;
    private FloatMolangExp3 offset = FloatMolangExp3.ZERO;
    private FloatMolangExp radius = FloatMolangExp.ONE;
    private FloatMolangExp3 halfDimensions = FloatMolangExp3.ZERO;
    private boolean surfaceOnly;
    private int directionMode = DIRECTION_OUTWARDS;
    private FloatMolangExp3 customDirection = FloatMolangExp3.ZERO;
    private int planeMode = PLANE_Y;
    private FloatMolangExp3 customPlane = FloatMolangExp3.ZERO;

    ShapeFields(Runnable structuralChange, @Nullable Supplier<String> prefix) {
        this.structuralChange = structuralChange;
        this.prefix = prefix;
    }

    ResourceLocation subtypeId() {
        return switch (this.subtype) {
            case SPHERE -> EmitterShape.Sphere.ID;
            case BOX -> EmitterShape.Box.ID;
            case DISC -> EmitterShape.Disc.ID;
            case ENTITY_AABB -> EmitterShape.EntityAABB.ID;
            default -> EmitterShape.Point.ID;
        };
    }

    /// 按当前子类型往 Grid 里加行（`type` 下拉 + 子类型字段 + 按需显隐的自定义方向 / 平面）。
    void addRows(ComponentGrid grid) {
        grid.select(this.label(grid, "type"), ComponentGrid.variantOptionLabels(IDS, "emitter_shape"), this.subtype, index -> {
            this.subtype = index;
            this.structuralChange.run();
        });
        switch (this.subtype) {
            case SPHERE -> {
                this.addOffsetRow(grid);
                grid.text(this.label(grid, "radius"), ComponentGrid.expText(this.radius), text -> {
                    this.radius = ComponentGrid.expOf(text);
                    grid.markChanged();
                });
                this.addDirectionRows(grid);
                this.addSurfaceOnlyRow(grid);
            }
            case BOX -> {
                this.addOffsetRow(grid);
                grid.vector(this.label(grid, "half_dimensions"), this.halfDimensions, value -> {
                    this.halfDimensions = value;
                    grid.markChanged();
                });
                this.addDirectionRows(grid);
                this.addSurfaceOnlyRow(grid);
            }
            case DISC -> {
                this.addOffsetRow(grid);
                grid.text(this.label(grid, "radius"), ComponentGrid.expText(this.radius), text -> {
                    this.radius = ComponentGrid.expOf(text);
                    grid.markChanged();
                });
                this.addPlaneRows(grid);
                this.addDirectionRows(grid);
                this.addSurfaceOnlyRow(grid);
            }
            case ENTITY_AABB -> {
                this.addDirectionRows(grid);
                this.addSurfaceOnlyRow(grid);
            }
            default -> {
                this.addOffsetRow(grid);
                this.addDirectionRows(grid);
            }
        }
    }

    EmitterShape build() {
        EmitterShape.Direction direction = switch (this.directionMode) {
            case 0 -> EmitterShape.Direction.INWARDS;
            case 1 -> EmitterShape.Direction.OUTWARDS;
            default -> new EmitterShape.Direction("custom", this.customDirection);
        };
        EmitterShape.Disc.PlaneNormal plane = switch (this.planeMode) {
            case 0 -> EmitterShape.Disc.PlaneNormal.X;
            case 1 -> EmitterShape.Disc.PlaneNormal.Y;
            case 2 -> EmitterShape.Disc.PlaneNormal.Z;
            default -> new EmitterShape.Disc.PlaneNormal("custom", this.customPlane);
        };
        return switch (this.subtype) {
            case SPHERE -> new EmitterShape.Sphere(this.offset, this.radius, direction, this.surfaceOnly);
            case BOX -> new EmitterShape.Box(this.offset, this.halfDimensions, direction, this.surfaceOnly);
            case DISC -> new EmitterShape.Disc(this.offset, this.radius, plane, direction, this.surfaceOnly);
            case ENTITY_AABB -> new EmitterShape.EntityAABB(direction, this.surfaceOnly);
            default -> new EmitterShape.Point(this.offset, direction);
        };
    }

    void load(EmitterShape shape) {
        this.subtype = switch (shape) {
            case EmitterShape.Sphere ignored -> SPHERE;
            case EmitterShape.Box ignored -> BOX;
            case EmitterShape.Disc ignored -> DISC;
            case EmitterShape.EntityAABB ignored -> ENTITY_AABB;
            default -> POINT;
        };
        switch (shape) {
            case EmitterShape.Sphere sphere -> {
                this.offset = sphere.offset;
                this.radius = sphere.radius;
                this.surfaceOnly = sphere.isSurfaceOnly();
                this.loadDirection(sphere.direction);
            }
            case EmitterShape.Box box -> {
                this.offset = box.offset;
                this.halfDimensions = box.halfDimensions;
                this.surfaceOnly = box.isSurfaceOnly();
                this.loadDirection(box.direction);
            }
            case EmitterShape.Disc disc -> {
                this.offset = disc.offset;
                this.radius = disc.radius;
                this.surfaceOnly = disc.isSurfaceOnly();
                this.loadPlane(disc.planeNormal);
                this.loadDirection(disc.direction);
            }
            case EmitterShape.EntityAABB shape1 -> {
                this.surfaceOnly = shape1.isSurfaceOnly();
                this.loadDirection(shape1.direction);
            }
            case EmitterShape.Point point -> {
                this.offset = point.offset;
                this.loadDirection(point.direction);
            }
            default -> {
            }
        }
    }

    private Supplier<String> label(ComponentGrid grid, String field) {
        return this.prefix == null ? () -> grid.fieldLabel(field) : () -> this.prefix.get() + field;
    }

    private void addOffsetRow(ComponentGrid grid) {
        grid.vector(this.label(grid, "offset"), this.offset, value -> {
            this.offset = value;
            grid.markChanged();
        });
    }

    private void addDirectionRows(ComponentGrid grid) {
        grid.select(this.label(grid, "direction"), ComponentGrid.localizedOptions("emitter_shape",
                DIRECTION_OPTIONS.stream().map(value -> "direction_" + value).toList()), this.directionMode, index -> {
            this.directionMode = index;
            // 「自定义」才需要自定义方向向量：切换后重建行
            this.structuralChange.run();
        });
        if (this.directionMode == DIRECTION_CUSTOM) {
            grid.vector(this.label(grid, "custom_direction"), this.customDirection, value -> {
                this.customDirection = value;
                grid.markChanged();
            });
        }
    }

    private void addPlaneRows(ComponentGrid grid) {
        grid.select(this.label(grid, "plane_normal"), ComponentGrid.localizedOptions("emitter_shape",
                PLANE_OPTIONS.stream().map(value -> "plane_" + value).toList()), this.planeMode, index -> {
            this.planeMode = index;
            // 「自定义」才需要自定义平面法线：切换后重建行
            this.structuralChange.run();
        });
        if (this.planeMode == PLANE_CUSTOM) {
            grid.vector(this.label(grid, "custom_plane"), this.customPlane, value -> {
                this.customPlane = value;
                grid.markChanged();
            });
        }
    }

    private void addSurfaceOnlyRow(ComponentGrid grid) {
        grid.toggle(this.label(grid, "surface_only"), this.surfaceOnly, value -> {
            this.surfaceOnly = value;
            grid.markChanged();
        });
    }

    private void loadDirection(EmitterShape.Direction direction) {
        this.directionMode = switch (direction.name()) {
            case "inwards" -> 0;
            case "outwards" -> 1;
            default -> DIRECTION_CUSTOM;
        };
        this.customDirection = direction.direct();
    }

    private void loadPlane(EmitterShape.Disc.PlaneNormal plane) {
        if (plane.isCustom()) {
            this.planeMode = PLANE_CUSTOM;
        } else {
            this.planeMode = switch (plane.name()) {
                case "x" -> 0;
                case "z" -> 2;
                default -> PLANE_Y;
            };
        }
        this.customPlane = plane.plane();
    }
}

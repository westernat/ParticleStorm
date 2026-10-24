package org.mesdag.particlestorm.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/// 粒子消除平面（`minecraft:particle_kill_plane`）的场景内修改手柄。
///
/// 只有 [ParticleKillPlaneGrid] 正在显示时才创建，并由 [SceneGizmo] 通路挂进场景：
/// - **平面本身**：半透明方块 + 平面内网格 + 法线箭头，让 `A/B/C/D` 的几何含义一眼可见；
/// - **方块手柄（平面中心）**：按住拖动 → 沿法线平移平面（只改 `D`）；屏幕拖动会先换算成世界位移，
///   只有沿法线的分量生效，所以任意方向拖都能用；
/// - **菱形手柄（法线箭头末端）**：按住拖动 → 把鼠标映射到以平面中心为球心的虚拟半球来旋转法线
///   （改 `A/B/C`），并以拖动开始时平面中心为锚点反算 `D`，平面不会在旋转时跳走。
///
/// 坐标约定与组件一致：平面相对**场景原点**（发射器位置）定义，`A*x + B*y + C*z + D = 0` 里的
/// `(x, y, z)` 是相对发射器的偏移，所以绘制时要把场景原点加回去。
///
/// 拖动过程只改 Grid 里的草稿值（文字行实时更新），松手才 `commit`（[ParticleKillPlaneGrid] 会
/// `markChanged` → 宿主重建组件并重新发射），避免拖动时每一帧都清场景重放粒子。
final class PlaneGizmo implements SceneGizmo {
    /// 手柄的屏幕半径（像素）与命中半径。
    private static final float HANDLE_RADIUS = 6.0F;
    private static final float HANDLE_HIT_RADIUS = 10.0F;
    private static final float HOVER_SCALE = 1.3F;
    /// 平面在视图里的半边长：跟随缩放，保证任何 zoom 下都占据视野的约 3/4。
    private static final float PLANE_SIZE_FACTOR = 0.75F;
    private static final float MIN_PLANE_HALF_SIZE = 0.6F;
    private static final float MAX_PLANE_HALF_SIZE = 12.0F;
    /// 法线长度相对平面半边长。
    private static final float ARROW_LENGTH_FACTOR = 0.6F;
    private static final int GRID_DIVISIONS = 4;
    /// 法线长度平方小于该值视为无效平面（`A=B=C=0`），不绘制也不响应拖动。
    private static final float DEGENERATE_NORMAL = 1.0E-8F;

    private static final int DRAG_NONE = 0;
    private static final int DRAG_MOVE = 1;
    private static final int DRAG_ROTATE = 2;

    private final EditorScreen screen;
    private final PlaneSource source;
    private final PlaneSink sink;
    private final Component hint = Component.translatable("particle_editor.gizmo.kill_plane");
    /// 旋转时固定住的锚点（拖动开始时的平面中心，发射器局部坐标）。
    private final Vector3f rotateAnchor = new Vector3f();
    /// 旋转手柄的虚拟半球：拖动开始时法线在屏幕上的半径（像素）、法线在相机空间的两个分量与朝向相机的符号。
    private float rotateRadius = 1.0F;
    private float rotateStartA;
    private float rotateStartB;
    private float rotateTowardSign = 1.0F;
    private double rotateStartMouseX;
    private double rotateStartMouseY;
    private int dragging = DRAG_NONE;
    /// 本次拖动是否真的改过值（只是点一下手柄再松开时不必重建组件）。
    private boolean dirty;
    private double lastMouseX;
    private double lastMouseY;

    PlaneGizmo(EditorScreen screen, PlaneSource source, PlaneSink sink) {
        this.screen = screen;
        this.source = source;
        this.sink = sink;
    }

    /// Grid 里的当前草稿平面。
    record Plane(float a, float b, float c, float d) {
        Vector3f normal() {
            return new Vector3f(this.a, this.b, this.c);
        }
    }

    @FunctionalInterface
    interface PlaneSource {
        Plane plane();
    }

    @FunctionalInterface
    interface PlaneSink {
        /// `commit` 为 false 时只更新草稿（拖动中），为 true 时才落地重建组件。
        void apply(Plane plane, boolean commit);
    }

    /// 一帧的几何缓存：单位法线、世界中心、平面内两轴、法线箭头末端与平面半边长。
    private record Geometry(Vector3f normal, Vec3 center, Vector3f centerLocal, Vec3 uAxis, Vec3 vAxis,
                            Vec3 arrowEnd, float halfSize) {
    }

    // region 渲染

    @Override
    public void render(int mouseX, int mouseY, Vec3 cameraPos) {
        Geometry geometry = this.geometry();
        if (geometry == null) {
            return;
        }
        int hovered = this.hitTest(mouseX, mouseY);
        int active = this.dragging != DRAG_NONE ? this.dragging : hovered;

        Vec3 right = this.screen.cameraRight();
        Vec3 up = this.screen.cameraUp();
        float worldPerPixel = this.screen.worldPerPixel();
        float moveRadius = HANDLE_RADIUS * worldPerPixel * (active == DRAG_MOVE ? HOVER_SCALE : 1.0F);
        float rotateRadius = HANDLE_RADIUS * worldPerPixel * (active == DRAG_ROTATE ? HOVER_SCALE : 1.0F);

        BufferBuilder quads = Tesselator.getInstance().getBuilder();
        quads.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        this.addPlaneFill(quads, cameraPos, geometry);
        this.addHandleQuads(quads, cameraPos, geometry, right, up, moveRadius, rotateRadius, active);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // 手柄要"透视"方块和粒子，和坐标轴一样关掉深度测试
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        if (cull) {
            RenderSystem.disableCull();
        }
        try {
            BufferUploader.drawWithShader(quads.end());
            // Tesselator 只有一个复用缓冲：必须先把上面的 quad 画掉，再 begin 线段
            BufferBuilder lines = Tesselator.getInstance().getBuilder();
            lines.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            this.addPlaneLines(lines, cameraPos, geometry);
            this.addHandleLines(lines, cameraPos, geometry, right, up, moveRadius, rotateRadius, active);
            BufferUploader.drawWithShader(lines.end());
        } finally {
            if (cull) {
                RenderSystem.enableCull();
            }
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        }
    }

    private void addPlaneFill(BufferBuilder builder, Vec3 cameraPos, Geometry geometry) {
        Vec3 u = geometry.uAxis().scale(geometry.halfSize());
        Vec3 v = geometry.vAxis().scale(geometry.halfSize());
        Vec3 center = geometry.center();
        this.addQuad(builder, cameraPos,
                center.subtract(u).subtract(v),
                center.add(u).subtract(v),
                center.add(u).add(v),
                center.subtract(u).add(v),
                0.35F, 0.65F, 1.0F, 0.10F);
    }

    private void addPlaneLines(BufferBuilder builder, Vec3 cameraPos, Geometry geometry) {
        Vec3 center = geometry.center();
        Vec3 u = geometry.uAxis().scale(geometry.halfSize());
        Vec3 v = geometry.vAxis().scale(geometry.halfSize());
        Vec3 c00 = center.subtract(u).subtract(v);
        Vec3 c10 = center.add(u).subtract(v);
        Vec3 c11 = center.add(u).add(v);
        Vec3 c01 = center.subtract(u).add(v);
        // 外框
        this.addLine(builder, cameraPos, c00, c10, 0.55F, 0.8F, 1.0F, 0.85F);
        this.addLine(builder, cameraPos, c10, c11, 0.55F, 0.8F, 1.0F, 0.85F);
        this.addLine(builder, cameraPos, c11, c01, 0.55F, 0.8F, 1.0F, 0.85F);
        this.addLine(builder, cameraPos, c01, c00, 0.55F, 0.8F, 1.0F, 0.85F);
        // 平面内网格
        for (int i = 1; i < GRID_DIVISIONS; i++) {
            float t = -1.0F + 2.0F * i / GRID_DIVISIONS;
            this.addLine(builder, cameraPos, center.add(u.scale(t)).subtract(v), center.add(u.scale(t)).add(v),
                    0.35F, 0.65F, 1.0F, 0.28F);
            this.addLine(builder, cameraPos, center.add(v.scale(t)).subtract(u), center.add(v.scale(t)).add(u),
                    0.35F, 0.65F, 1.0F, 0.28F);
        }
        // 法线箭头
        Vec3 normal = new Vec3(geometry.normal().x, geometry.normal().y, geometry.normal().z);
        this.addLine(builder, cameraPos, center, geometry.arrowEnd(), 1.0F, 0.85F, 0.2F, 0.95F);
        Vec3 side = geometry.uAxis().scale(geometry.halfSize() * 0.12F);
        Vec3 back = normal.scale(geometry.halfSize() * 0.18F);
        this.addLine(builder, cameraPos, geometry.arrowEnd(), geometry.arrowEnd().subtract(back).add(side),
                1.0F, 0.85F, 0.2F, 0.95F);
        this.addLine(builder, cameraPos, geometry.arrowEnd(), geometry.arrowEnd().subtract(back).subtract(side),
                1.0F, 0.85F, 0.2F, 0.95F);
    }

    private void addHandleQuads(BufferBuilder builder, Vec3 cameraPos, Geometry geometry, Vec3 right, Vec3 up,
                                float moveRadius, float rotateRadius, int active) {
        Vec3 moveRight = right.scale(moveRadius);
        Vec3 moveUp = up.scale(moveRadius);
        Vec3 center = geometry.center();
        this.addQuad(builder, cameraPos,
                center.subtract(moveRight).subtract(moveUp),
                center.add(moveRight).subtract(moveUp),
                center.add(moveRight).add(moveUp),
                center.subtract(moveRight).add(moveUp),
                active == DRAG_MOVE ? 1.0F : 0.95F,
                active == DRAG_MOVE ? 0.95F : 0.78F,
                active == DRAG_MOVE ? 0.45F : 0.20F, 1.0F);

        Vec3 rotateRight = right.scale(rotateRadius);
        Vec3 rotateUp = up.scale(rotateRadius);
        Vec3 end = geometry.arrowEnd();
        this.addQuad(builder, cameraPos,
                end.add(rotateUp),
                end.add(rotateRight),
                end.subtract(rotateUp),
                end.subtract(rotateRight),
                active == DRAG_ROTATE ? 0.75F : 0.35F, 0.95F, 1.0F, 1.0F);
    }

    private void addHandleLines(BufferBuilder builder, Vec3 cameraPos, Geometry geometry, Vec3 right, Vec3 up,
                                float moveRadius, float rotateRadius, int active) {
        Vec3 moveRight = right.scale(moveRadius);
        Vec3 moveUp = up.scale(moveRadius);
        Vec3 center = geometry.center();
        Vec3 m00 = center.subtract(moveRight).subtract(moveUp);
        Vec3 m10 = center.add(moveRight).subtract(moveUp);
        Vec3 m11 = center.add(moveRight).add(moveUp);
        Vec3 m01 = center.subtract(moveRight).add(moveUp);
        float border = active == DRAG_MOVE ? 1.0F : 0.9F;
        this.addLine(builder, cameraPos, m00, m10, 1.0F, 1.0F, border, 0.9F);
        this.addLine(builder, cameraPos, m10, m11, 1.0F, 1.0F, border, 0.9F);
        this.addLine(builder, cameraPos, m11, m01, 1.0F, 1.0F, border, 0.9F);
        this.addLine(builder, cameraPos, m01, m00, 1.0F, 1.0F, border, 0.9F);

        Vec3 rotateRight = right.scale(rotateRadius);
        Vec3 rotateUp = up.scale(rotateRadius);
        Vec3 end = geometry.arrowEnd();
        Vec3 r0 = end.add(rotateUp);
        Vec3 r1 = end.add(rotateRight);
        Vec3 r2 = end.subtract(rotateUp);
        Vec3 r3 = end.subtract(rotateRight);
        float rotateBorder = active == DRAG_ROTATE ? 1.0F : 0.9F;
        this.addLine(builder, cameraPos, r0, r1, 0.85F, 1.0F, rotateBorder, 0.9F);
        this.addLine(builder, cameraPos, r1, r2, 0.85F, 1.0F, rotateBorder, 0.9F);
        this.addLine(builder, cameraPos, r2, r3, 0.85F, 1.0F, rotateBorder, 0.9F);
        this.addLine(builder, cameraPos, r3, r0, 0.85F, 1.0F, rotateBorder, 0.9F);
    }

    private void addLine(BufferBuilder builder, Vec3 cameraPos, Vec3 from, Vec3 to,
                         float red, float green, float blue, float alpha) {
        builder.vertex((float) (from.x - cameraPos.x), (float) (from.y - cameraPos.y), (float) (from.z - cameraPos.z))
                .color(red, green, blue, alpha)
                .endVertex();
        builder.vertex((float) (to.x - cameraPos.x), (float) (to.y - cameraPos.y), (float) (to.z - cameraPos.z))
                .color(red, green, blue, alpha)
                .endVertex();
    }

    private void addQuad(BufferBuilder builder, Vec3 cameraPos, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                         float red, float green, float blue, float alpha) {
        this.addVertex(builder, cameraPos, p0, red, green, blue, alpha);
        this.addVertex(builder, cameraPos, p1, red, green, blue, alpha);
        this.addVertex(builder, cameraPos, p2, red, green, blue, alpha);
        this.addVertex(builder, cameraPos, p3, red, green, blue, alpha);
    }

    private void addVertex(BufferBuilder builder, Vec3 cameraPos, Vec3 point,
                           float red, float green, float blue, float alpha) {
        builder.vertex((float) (point.x - cameraPos.x), (float) (point.y - cameraPos.y), (float) (point.z - cameraPos.z))
                .color(red, green, blue, alpha)
                .endVertex();
    }

    // endregion

    // region 拖动

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        Geometry geometry = this.geometry();
        if (geometry == null) {
            return false;
        }
        int hit = this.hitTest(geometry, mouseX, mouseY);
        if (hit == DRAG_NONE) {
            return false;
        }
        this.dragging = hit;
        this.dirty = false;
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        if (hit == DRAG_ROTATE) {
            this.rotateAnchor.set(geometry.centerLocal());
            this.rotateRadius = Math.max(1.0F, geometry.halfSize() * ARROW_LENGTH_FACTOR / this.screen.worldPerPixel());
            this.rotateStartMouseX = mouseX;
            this.rotateStartMouseY = mouseY;
            Vec3 right = this.screen.cameraRight();
            Vec3 up = this.screen.cameraUp();
            this.rotateStartA = (float) dot(geometry.normal(), right);
            this.rotateStartB = (float) dot(geometry.normal(), up);
            // 法线朝向相机 / 背离相机决定虚拟半球的 z 方向（两者投影相同）
            Vec3 forward = this.screen.cameraForward();
            this.rotateTowardSign = dot(geometry.normal(), forward) > 0.0D ? -1.0F : 1.0F;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY) {
        if (this.dragging == DRAG_NONE) {
            return false;
        }
        double deltaX = mouseX - this.lastMouseX;
        double deltaY = mouseY - this.lastMouseY;
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        if (deltaX == 0.0D && deltaY == 0.0D) {
            return true;
        }
        Plane plane = this.source.plane();
        Vector3f normal = plane.normal();
        if (normal.lengthSquared() < DEGENERATE_NORMAL) {
            return true;
        }
        if (this.dragging == DRAG_MOVE) {
            // 屏幕位移 → 世界位移（正交投影下 1px = worldPerPixel），只取沿法线的分量改 D
            float worldPerPixel = this.screen.worldPerPixel();
            Vec3 movement = this.screen.cameraRight().scale(deltaX * worldPerPixel)
                    .add(this.screen.cameraUp().scale(-deltaY * worldPerPixel));
            float alongNormal = (float) movement.dot(new Vec3(normal.x, normal.y, normal.z));
            this.sink.apply(new Plane(plane.a(), plane.b(), plane.c(), plane.d() - alongNormal), false);
            this.dirty = true;
        } else {
            // 把鼠标映射到以平面中心为球心的虚拟半球：拖向圆内 = 法线朝相机倾斜，
            // 沿圆周拖 = 法线在屏幕平面内旋转；再以拖动开始时的平面中心反算 D，平面原地转不跳。
            double dx = mouseX - this.rotateStartMouseX;
            double dy = mouseY - this.rotateStartMouseY;
            double a = this.rotateStartA + dx / this.rotateRadius;
            double b = this.rotateStartB - dy / this.rotateRadius;
            double radiusSqr = a * a + b * b;
            if (radiusSqr > 0.9999D) {
                double scale = Math.sqrt(0.9999D / radiusSqr);
                a *= scale;
                b *= scale;
                radiusSqr = 0.9999D;
            }
            double toward = this.rotateTowardSign * Math.sqrt(Math.max(0.0D, 1.0D - radiusSqr));
            Vec3 right = this.screen.cameraRight();
            Vec3 up = this.screen.cameraUp();
            Vec3 viewer = this.screen.cameraForward().scale(-1.0D);
            Vector3f rotated = new Vector3f(
                    (float) (right.x * a + up.x * b + viewer.x * toward),
                    (float) (right.y * a + up.y * b + viewer.y * toward),
                    (float) (right.z * a + up.z * b + viewer.z * toward)).normalize();
            float d = -rotated.dot(this.rotateAnchor);
            this.sink.apply(new Plane(rotated.x, rotated.y, rotated.z, d), false);
            this.dirty = true;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY) {
        if (this.dragging == DRAG_NONE) {
            return false;
        }
        boolean commit = this.dirty;
        this.dragging = DRAG_NONE;
        this.dirty = false;
        if (commit) {
            // 松手才落地：重建组件 + 重新发射，拖动过程不会一直清场景
            this.sink.apply(this.source.plane(), true);
        }
        return true;
    }

    @Override
    public boolean isDragging() {
        return this.dragging != DRAG_NONE;
    }

    @Override
    public Component hint() {
        return this.hint;
    }

    /// 关掉 Grid / 切页签时丢弃拖动状态；拖动中改过的值仍按「松手」处理，避免草稿和组件不一致。
    void cancelDrag() {
        if (this.dragging == DRAG_NONE) {
            return;
        }
        this.mouseReleased(this.lastMouseX, this.lastMouseY);
    }

    // endregion

    // region 几何与命中

    private Geometry geometry() {
        Plane plane = this.source.plane();
        Vector3f normal = plane.normal();
        float lengthSqr = normal.lengthSquared();
        if (lengthSqr < DEGENERATE_NORMAL) {
            return null;
        }
        float length = Mth.sqrt(lengthSqr);
        Vector3f unit = new Vector3f(normal).div(length);
        // 平面到发射器（场景原点）的最近点：p = -D/|n|² * n
        Vector3f centerLocal = new Vector3f(unit).mul(-plane.d() / length);
        Vec3 origin = this.screen.sceneOriginPos();
        Vec3 center = origin.add(centerLocal.x, centerLocal.y, centerLocal.z);
        Vector3f reference = Math.abs(unit.y) < 0.9F ? new Vector3f(0.0F, 1.0F, 0.0F) : new Vector3f(1.0F, 0.0F, 0.0F);
        Vector3f uAxis = new Vector3f(unit).cross(reference).normalize();
        Vector3f vAxis = new Vector3f(unit).cross(uAxis).normalize();
        float halfSize = Mth.clamp(this.screen.sceneHalfHeight() * PLANE_SIZE_FACTOR,
                MIN_PLANE_HALF_SIZE, MAX_PLANE_HALF_SIZE);
        Vec3 arrowEnd = center.add(unit.x * halfSize * ARROW_LENGTH_FACTOR,
                unit.y * halfSize * ARROW_LENGTH_FACTOR,
                unit.z * halfSize * ARROW_LENGTH_FACTOR);
        return new Geometry(unit, center, centerLocal, new Vec3(uAxis.x, uAxis.y, uAxis.z),
                new Vec3(vAxis.x, vAxis.y, vAxis.z), arrowEnd, halfSize);
    }

    private int hitTest(double mouseX, double mouseY) {
        Geometry geometry = this.geometry();
        return geometry == null ? DRAG_NONE : this.hitTest(geometry, mouseX, mouseY);
    }

    private int hitTest(Geometry geometry, double mouseX, double mouseY) {
        Vector2f center = this.screen.projectToScreen(geometry.center());
        Vector2f arrowEnd = this.screen.projectToScreen(geometry.arrowEnd());
        double centerDistance = squaredDistance(mouseX, mouseY, center.x, center.y);
        double arrowDistance = squaredDistance(mouseX, mouseY, arrowEnd.x, arrowEnd.y);
        double hitRadiusSqr = (double) HANDLE_HIT_RADIUS * HANDLE_HIT_RADIUS;
        // 箭头末端离屏幕中心太近时两个手柄会重叠：优先旋转手柄
        if (arrowDistance <= hitRadiusSqr && arrowDistance <= centerDistance) {
            return DRAG_ROTATE;
        }
        return centerDistance <= hitRadiusSqr ? DRAG_MOVE : DRAG_NONE;
    }

    private static double squaredDistance(double x0, double y0, double x1, double y1) {
        double dx = x0 - x1;
        double dy = y0 - y1;
        return dx * dx + dy * dy;
    }

    private static double dot(Vector3f vector, Vec3 axis) {
        return vector.x * axis.x + vector.y * axis.y + vector.z * axis.z;
    }

    // endregion
}

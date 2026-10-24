package org.mesdag.particlestorm.editor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/// 场景内的 3D 操作手柄：组件 Grid 想在预览场景里画可拖拽的辅助图形时实现它。
///
/// 生命周期跟着打开的 [ComponentGrid] 走：只有 Grid 正在显示时 [EditorTab#sceneGizmo] 才返回它，
/// 关闭 Grid / 切走页签后返回 null，场景里就不再绘制、也不再响应鼠标。
///
/// 坐标约定与场景里的粒子一致：绘制时 [EditorScreen] 已经把投影/model-view 换成编辑器的
/// 正交投影 + 相机视图矩阵，顶点要写**相机相对坐标**；命中判定则用 [EditorScreen#projectToScreen] 投影回屏幕。
interface SceneGizmo {
    /// 在场景 pass 的末尾绘制（方块/粒子/坐标轴之后，场景 scissor 之内）。
    void render(int mouseX, int mouseY, Vec3 cameraPos);

    /// 场景内点击；返回 true 表示命中手柄，本次点击不再触发相机操作。
    boolean mouseClicked(double mouseX, double mouseY, int button);

    /// 按住左键拖动；返回 true 表示手柄正在拖动。
    boolean mouseDragged(double mouseX, double mouseY);

    /// 松开左键；返回 true 表示手柄曾处于拖动状态。
    boolean mouseReleased(double mouseX, double mouseY);

    boolean isDragging();

    /// 场景左上角的操作提示（由 [EditorScreen] 在 overlay 里绘制）。
    Component hint();
}

package org.mesdag.particlestorm.editor;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

/// 编辑器预览用的轨道相机。
///
/// 只负责“把 焦点 + yaw/pitch + 距离 换算成 [Camera] 的位置和朝向”：
/// [Camera] 的 `setPosition`/`setRotation` 是 protected，所以用子类调用；
/// 其余（左键旋转、右键平移、滚轮缩放）都由 [EditorScreen] 覆写 `Screen` 的鼠标方法完成。
///
/// 约定与 `GameRenderer#renderLevel` 一致：相机视图矩阵 = `rotation(conjugate(camera.rotation()))`，
/// 粒子顶点本来就是**相机相对坐标**，因此正交/透视投影都直接可用。
final class PreviewCamera extends Camera {
    /// 让相机沿 `yaw/pitch` 方向看向 `focus`，并与 `focus` 相距 `distance`。
    ///
    /// @param yawDegrees   水平角（MC 约定：0 朝 +Z，90 朝 -X）
    /// @param pitchDegrees 俯仰角（正数向下看），调用方负责夹在 [-89, 89]
    /// @param distance     相机到焦点的距离（透视模式按 FOV 计算；正交模式只影响裁剪）
    public void orbit(Vec3 focus, float yawDegrees, float pitchDegrees, double distance) {
        Vec3 forward = forwardOf(yawDegrees, pitchDegrees);
        Vec3 position = focus.subtract(forward.scale(distance));
        super.setPosition(position.x, position.y, position.z);
        super.setRotation(yawDegrees, pitchDegrees);
    }

    /// 相机空间 (0,0,-1)（正前方）旋转到世界空间后的方向。
    public static Vec3 forwardOf(float yawDegrees, float pitchDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        return new Vec3(
                -Math.sin(yaw) * Math.cos(pitch),
                -Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch)
        );
    }

    /// 相机空间 right 轴旋转到世界空间后的方向（屏幕向右）。
    public static Vec3 rightOf(float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        return new Vec3(-Math.cos(yaw), 0.0D, -Math.sin(yaw));
    }

    /// 相机空间 up 轴旋转到世界空间后的方向（屏幕向上）。
    public static Vec3 upOf(float yawDegrees, float pitchDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        return new Vec3(
                -Math.sin(yaw) * Math.sin(pitch),
                Math.cos(pitch),
                Math.cos(yaw) * Math.sin(pitch)
        );
    }

    @Override
    public boolean isInitialized() {
        // 预览相机不走 Camera#setup（没有绑定实体），这里始终认为它已就绪
        return true;
    }
}

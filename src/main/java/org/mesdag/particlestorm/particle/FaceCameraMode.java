package org.mesdag.particlestorm.particle;

import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.data.MathHelper;
import org.mesdag.particlestorm.data.component.ParticleAppearanceBillboard;

/// 每个模式都在**粒子所在的空间**里计算朝向：
/// 本地空间粒子（`local_rotation` 为 true）算出的是发射器本地空间里的朝向，
/// 再由 [MolangParticleInstance#render] 统一绕粒子中心变换到世界空间。
/// 因此那些依赖摄像机的模式必须先把摄像机的方向转进这个空间，否则最后乘上本地旋转就会把粒子转得不再朝向摄像机。
public enum FaceCameraMode {
    DO_NOTHING {
        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.identity();
        }
    },
    LOOKAT_XYZ {
        private static final Vector3f wd = new Vector3f();
        private static final Vector3f qd = new Vector3f();
        private static final Vector3f up = new Vector3f(0, 1, 0);
        private static final Vector3f worldPos = new Vector3f();
        private static final Matrix3f mat = new Matrix3f();

        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            // 本地空间粒子的坐标是本地坐标，要先变换到世界坐标才能得到正确的方向
            Vector3f xd = toParticleSpace(instance, camera.getPosition().toVector3f().sub(
                    instance.getWorldPosition(worldPos, partialTick)
            )).normalize();
            up.cross(xd, wd).normalize();
            xd.cross(wd, qd);
            quaternion.setFromNormalized(mat.set(
                    wd.x, qd.x, xd.x,
                    wd.y, qd.y, xd.y,
                    wd.z, qd.z, xd.z
            ).invert());
        }
    },
    LOOKAT_Y {
        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            LOOKAT_XYZ.setRotation(instance, quaternion, camera, partialTick);
            quaternion.x = 0.0F;
            quaternion.z = 0.0F;
        }
    },
    ROTATE_XYZ {
        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.set(camera.rotation());
            toParticleSpace(instance, quaternion);
        }
    },
    ROTATE_Y {
        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.set(0.0F, camera.rotation().y, 0.0F, camera.rotation().w);
            toParticleSpace(instance, quaternion);
        }
    },
    DIRECTION_X {
        private static final Vector3f defaultDir = new Vector3f(0, 0, -1);
        private static final Quaternionf dirRot = new Quaternionf();

        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.rotationXYZ(0.0F, Mth.HALF_PI, 0.0F);
            MathHelper.setFromUnitVectors(defaultDir, instance.getFacingDirection(), dirRot);
            quaternion.premul(dirRot);
        }
    },
    DIRECTION_Y {
        private static final Vector3f defaultDir = new Vector3f(0, 0, -1);
        private static final Quaternionf dirRot = new Quaternionf();

        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.rotationXYZ(Mth.HALF_PI, Mth.PI, 0.0F);
            MathHelper.setFromUnitVectors(defaultDir, instance.getFacingDirection(), dirRot);
            quaternion.premul(dirRot);
        }
    },
    DIRECTION_Z {
        private static final Vector3f defaultDir = new Vector3f(0, 0, -1);
        private static final Quaternionf dirRot = new Quaternionf();

        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.rotationXYZ(0.0F, 0.0F, 0.0F);
            MathHelper.setFromUnitVectors(defaultDir, instance.getFacingDirection(), dirRot);
            quaternion.premul(dirRot);
        }
    },
    LOOKAT_DIRECTION {
        private static final Vector3f X = new Vector3f(1.0F, 0.0F, 0.0F);
        private static final Vector3f worldPos = new Vector3f();
        private static final Vector4f t = new Vector4f();
        private static final Matrix4f m = new Matrix4f();

        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            MathHelper.setFromUnitVectors(X, instance.getFacingDirection(), quaternion);
            Vec3 pos = camera.getPosition();
            instance.getWorldPosition(worldPos, partialTick);
            t.set(
                    pos.x - worldPos.x,
                    pos.y - worldPos.y,
                    pos.z - worldPos.z,
                    0
            );
            toParticleSpace(instance, t).mul(m.rotation(quaternion).invert());
            quaternion.rotateX((float) Mth.atan2(-t.y, t.z));
        }
    },
    EMITTER_TRANSFORM_XY {
        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.rotationXYZ(0.0F, 0.0F, 0.0F);
        }
    },
    EMITTER_TRANSFORM_XZ {
        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.rotationXYZ(Mth.HALF_PI, 0.0F, 0.0F);
        }
    },
    EMITTER_TRANSFORM_YZ {
        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.rotationXYZ(0.0F, -Mth.HALF_PI, 0.0F);
        }
    };

    private static final Quaternionf spaceRot = new Quaternionf();

    /// 把世界空间的方向转换到粒子所在的空间（本地空间粒子就是发射器本地空间）
    private static Vector3f toParticleSpace(IMolangParticleInstance instance, Vector3f vec) {
        Quaternionf rotation = instance.getLocalSpaceRotation();
        return rotation == null ? vec : vec.rotate(spaceRot.set(rotation).conjugate());
    }

    /// 把世界空间的方向转换到粒子所在的空间，只转换 xyz
    private static Vector4f toParticleSpace(IMolangParticleInstance instance, Vector4f vec) {
        Quaternionf rotation = instance.getLocalSpaceRotation();
        return rotation == null ? vec : vec.rotate(spaceRot.set(rotation).conjugate());
    }

    /// 把世界空间的旋转转换到粒子所在的空间
    private static Quaternionf toParticleSpace(IMolangParticleInstance instance, Quaternionf rot) {
        Quaternionf rotation = instance.getLocalSpaceRotation();
        return rotation == null ? rot : rot.premul(spaceRot.set(rotation).conjugate());
    }

    public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {}

    public static FaceCameraMode fromComponent(ParticleAppearanceBillboard.FaceCameraMode faceCameraMode) {
        try {
            return FaceCameraMode.valueOf(faceCameraMode.name());
        } catch (Exception e) {
            return DO_NOTHING;
        }
    }
}

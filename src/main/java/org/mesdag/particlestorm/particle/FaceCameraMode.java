package org.mesdag.particlestorm.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.data.MathHelper;
import org.mesdag.particlestorm.data.component.ParticleAppearanceBillboard;

public enum FaceCameraMode implements SingleQuadParticle.FacingCameraMode {
    DO_NOTHING {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {}
    },
    LOOKAT_XYZ {
        private static final Vector3f wd = new Vector3f();
        private static final Vector3f qd = new Vector3f();
        private static final Vector3f up = new Vector3f(0, 1, 0);
        private static final Matrix3f mat = new Matrix3f();

        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {}

        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            Vector3f xd = camera.position().toVector3f().sub(
                    (float) instance.getX(),
                    (float) instance.getY(),
                    (float) instance.getZ()
            ).normalize();
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
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {}

        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            LOOKAT_XYZ.setRotation(instance, quaternion, camera, partialTick);
            quaternion.x = 0.0F;
            quaternion.z = 0.0F;
        }
    },
    ROTATE_XYZ {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.set(camera.rotation());
        }
    },
    ROTATE_Y {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.set(0.0F, camera.rotation().y, 0.0F, camera.rotation().w);
        }
    },
    DIRECTION_X {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.rotationXYZ(0.0F, Mth.HALF_PI, 0.0F);
        }

        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            if (!setDirectionXRotation(instance, quaternion)) {
                setRotation(quaternion, camera, partialTick);
            }
        }
    },
    DIRECTION_Y {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.rotationXYZ(Mth.HALF_PI, Mth.PI, 0.0F);
        }

        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            if (!setDirectionYRotation(instance, quaternion)) {
                setRotation(quaternion, camera, partialTick);
            }
        }
    },
    DIRECTION_Z {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.rotationXYZ(0.0F, 0.0F, 0.0F);
        }

        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            if (!setDirectionZRotation(instance, quaternion)) {
                setRotation(quaternion, camera, partialTick);
            }
        }
    },
    LOOKAT_DIRECTION {
        private static final Vector3f X = new Vector3f(1.0F, 0.0F, 0.0F);
        private static final Vector4f t = new Vector4f();
        private static final Matrix4f m = new Matrix4f();

        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {}

        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            MathHelper.setFromUnitVectors(X, instance.getFacingDirection(), quaternion);
            Vec3 pos = camera.position();
            t.set(
                    pos.x - instance.getX(),
                    pos.y - instance.getY(),
                    pos.z - instance.getZ(),
                    0
            ).mul(m.rotation(quaternion).invert());
            quaternion.rotateX((float) Mth.atan2(-t.y, t.z));
        }
    },
    EMITTER_TRANSFORM_XY {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.rotationXYZ(0.0F, 0.0F, 0.0F);
        }
    },
    EMITTER_TRANSFORM_XZ {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.rotationXYZ(Mth.HALF_PI, 0.0F, 0.0F);
        }
    },
    EMITTER_TRANSFORM_YZ {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.rotationXYZ(0.0F, -Mth.HALF_PI, 0.0F);
        }
    };

    public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
        setRotation(quaternion, camera, partialTick);
    }

    private static final float DIRECTION_EPSILON = 1.0E-6F;
    private static final Vector3f WORLD_UP = new Vector3f(0.0F, 1.0F, 0.0F);
    private static final Vector3f FALLBACK_UP = new Vector3f(0.0F, 0.0F, 1.0F);
    private static final Vector3f AXIS_X = new Vector3f();
    private static final Vector3f AXIS_Y = new Vector3f();
    private static final Vector3f AXIS_Z = new Vector3f();
    private static final Vector3f TEMP = new Vector3f();
    private static final Matrix3f DIRECTION_MATRIX = new Matrix3f();

    private static boolean setDirectionXRotation(IMolangParticleInstance instance, Quaternionf quaternion) {
        if (!readDirection(instance, AXIS_X)) {
            return false;
        }

        setUpAxis(AXIS_X, AXIS_Y);
        AXIS_Z.set(AXIS_X).cross(AXIS_Y).normalize();
        setFromAxes(quaternion, AXIS_X, AXIS_Y, AXIS_Z);
        return true;
    }

    private static boolean setDirectionYRotation(IMolangParticleInstance instance, Quaternionf quaternion) {
        if (!readDirection(instance, AXIS_Y)) {
            return false;
        }

        setUpAxis(AXIS_Y, AXIS_X);
        AXIS_Z.set(AXIS_X).cross(AXIS_Y).normalize();
        setFromAxes(quaternion, AXIS_X, AXIS_Y, AXIS_Z);
        return true;
    }

    private static boolean setDirectionZRotation(IMolangParticleInstance instance, Quaternionf quaternion) {
        if (!readDirection(instance, AXIS_Z)) {
            return false;
        }

        setUpAxis(AXIS_Z, AXIS_Y);
        AXIS_X.set(AXIS_Y).cross(AXIS_Z).normalize();
        setFromAxes(quaternion, AXIS_X, AXIS_Y, AXIS_Z);
        return true;
    }

    private static boolean readDirection(IMolangParticleInstance instance, Vector3f dest) {
        dest.set(instance.getFacingDirection());
        if (dest.lengthSquared() <= DIRECTION_EPSILON) {
            return false;
        }
        dest.normalize();
        return true;
    }

    private static void setUpAxis(Vector3f lockedAxis, Vector3f dest) {
        Vector3f up = java.lang.Math.abs(WORLD_UP.dot(lockedAxis)) > 0.999F ? FALLBACK_UP : WORLD_UP;
        dest.set(up).sub(TEMP.set(lockedAxis).mul(up.dot(lockedAxis))).normalize();
    }

    private static void setFromAxes(Quaternionf quaternion, Vector3f xAxis, Vector3f yAxis, Vector3f zAxis) {
        quaternion.setFromNormalized(DIRECTION_MATRIX.set(
                xAxis.x, yAxis.x, zAxis.x,
                xAxis.y, yAxis.y, zAxis.y,
                xAxis.z, yAxis.z, zAxis.z
        ).invert());
    }

    public static FaceCameraMode fromComponent(ParticleAppearanceBillboard.FaceCameraMode faceCameraMode) {
        try {
            return FaceCameraMode.valueOf(faceCameraMode.name());
        } catch (Exception e) {
            return DO_NOTHING;
        }
    }
}

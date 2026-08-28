package org.mesdag.particlestorm.particle;

import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.data.MathHelper;
import org.mesdag.particlestorm.data.component.ParticleAppearanceBillboard;

public enum FaceCameraMode {
    DO_NOTHING {},
    LOOKAT_XYZ {
        private static final Vector3f wd = new Vector3f();
        private static final Vector3f qd = new Vector3f();
        private static final Vector3f up = new Vector3f(0, 1, 0);
        private static final Matrix3f mat = new Matrix3f();

        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            Vector3f xd = camera.getPosition().toVector3f().sub(
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
        }
    },
    ROTATE_Y {
        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.set(0.0F, camera.rotation().y, 0.0F, camera.rotation().w);
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
        private static final Vector4f t = new Vector4f();
        private static final Matrix4f m = new Matrix4f();

        @Override
        public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {
            MathHelper.setFromUnitVectors(X, instance.getFacingDirection(), quaternion);
            Vec3 pos = camera.getPosition();
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

    public void setRotation(IMolangParticleInstance instance, Quaternionf quaternion, Camera camera, float partialTick) {}

    public static FaceCameraMode fromComponent(ParticleAppearanceBillboard.FaceCameraMode faceCameraMode) {
        try {
            return FaceCameraMode.valueOf(faceCameraMode.name());
        } catch (Exception e) {
            return DO_NOTHING;
        }
    }
}

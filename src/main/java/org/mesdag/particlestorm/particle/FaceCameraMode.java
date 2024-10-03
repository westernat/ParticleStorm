package org.mesdag.particlestorm.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public enum FaceCameraMode implements SingleQuadParticle.FacingCameraMode {
    LOOKAT_XYZ {
        private final Vector3f wd = new Vector3f();
        private final Vector3f qd = new Vector3f();
        private final Vector3f up = new Vector3f(0, 1, 0);

        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {}

        @Override
        public void setRotation(float x, float y, float z, Quaternionf quaternion, Camera camera, float partialTick) {
            Vector3f xd = camera.getPosition().toVector3f().sub(x, y, z).normalize();
            up.cross(xd, wd).normalize();
            xd.cross(wd, qd);
            quaternion.setFromNormalized(new Matrix4f(
                    wd.x, qd.x, xd.x, 0,
                    wd.y, qd.y, xd.y, 0,
                    wd.z, qd.z, xd.z, 0,
                    0, 0, 0, 1
            ).invert());
        }
    },
    LOOKAT_Y {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {}

        @Override
        public void setRotation(float x, float y, float z, Quaternionf quaternion, Camera camera, float partialTick) {
            LOOKAT_XYZ.setRotation(x, y, z, quaternion, camera, partialTick);
            quaternion.x = 0.0F;
            quaternion.z = 0.0F;
        }
    },
    ROTATE_XYZ {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.premul(camera.rotation());
        }
    },
    ROTATE_Y {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.premul(0.0F, camera.rotation().y, 0.0F, camera.rotation().w);
        }
    },
    DIRECTION_X {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.rotationXYZ(0.0F, Mth.HALF_PI, 0.0F);
        }
    },
    DIRECTION_Y {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.rotationXYZ(Mth.HALF_PI, Mth.PI, 0.0F);
        }
    },
    DIRECTION_Z {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.rotationXYZ(0.0F, 0.0F, 0.0F);
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

    public void setRotation(float x, float y, float z, Quaternionf quaternion, Camera camera, float partialTick) {
        setRotation(quaternion, camera, partialTick);
    }
}

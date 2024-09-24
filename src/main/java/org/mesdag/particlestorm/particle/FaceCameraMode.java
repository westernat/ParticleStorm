package org.mesdag.particlestorm.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public enum FaceCameraMode implements SingleQuadParticle.FacingCameraMode {
    ROTATE_XYZ {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.premul(camera.rotation());
        }
    },
    ROTATE_Y {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            quaternion.premul(0.0F, -camera.rotation().y, 0.0F, 1.0F);
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
            // todo
        }
    },
    EMITTER_TRANSFORM_XZ {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            // todo
        }
    },
    EMITTER_TRANSFORM_YZ {
        @Override
        public void setRotation(Quaternionf quaternion, Camera camera, float partialTick) {
            // todo
        }
    }
}

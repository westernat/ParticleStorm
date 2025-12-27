package org.mesdag.particlestorm.data.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.Deserializer;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.FloatMolangExp2;
import org.mesdag.particlestorm.data.molang.FloatMolangExp3;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record ParticleAppearanceBillboard(FloatMolangExp2 size, FaceCameraMode faceCameraMode, Direction direction, UV uv) implements IParticleComponent {
    public static final ResourceLocation ID = new ResourceLocation("particle_appearance_billboard");

    @Override
    public Deserializer<ParticleAppearanceBillboard> deserializer() {
        return ParticleAppearanceBillboard::fromJson;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(
                size.exp1(), size.exp2(),
                direction.customDirection.exp1(), direction.customDirection.exp2(), direction.customDirection.exp3(),
                uv.uv.exp1(), uv.uv.exp2(),
                uv.uvSize.exp1(), uv.uvSize.exp2(),
                uv.flipbook.baseUV.exp1(), uv.flipbook.baseUV.exp2(),
                uv.flipbook.sizeUV.exp1(), uv.flipbook.sizeUV.exp2(),
                uv.flipbook.stepUV.exp1(), uv.flipbook.stepUV.exp2(),
                uv.flipbook.maxFrame
        );
    }

    @Override
    public void update(IMolangParticleInstance instance) {
        doFacingCameraMode(instance);
        doSize(instance);
        if (uv == UV.EMPTY) return;
        UV.Flipbook flipbook = uv.flipbook;
        if (flipbook == UV.Flipbook.EMPTY) {
            updateSimpleUV(instance);
        } else if (flipbook.stretchToLifetime) {
            updateFlipbookUV(instance);
            instance.setMaxFrame((int) flipbook.maxFrame.calculate(instance));
            instance.setCurrentFrame(instance.getMaxFrame() * instance.getAge() / instance.self().getLifetime());
        } else {
            float gameTime = (float) ((int) instance.getLevel().getGameTime() & 0b11111111);
            if (gameTime % (PSGameClient.tickRate() / flipbook.framesPerSecond) < 1.0F) {
                updateFlipbookUV(instance);
                instance.setMaxFrame((int) flipbook.maxFrame.calculate(instance));
                int currentFrame = instance.getCurrentFrame() + 1;
                if (currentFrame < instance.getMaxFrame()) {
                    instance.setCurrentFrame(currentFrame);
                } else {
                    instance.setCurrentFrame(flipbook.loop ? 0 : instance.getMaxFrame() - 1);
                }
            }
        }
    }

    @Override
    public void apply(IMolangParticleInstance instance) {
        doFacingCameraMode(instance);
        doSize(instance);
        if (uv == UV.EMPTY) return;
        if (uv.flipbook == UV.Flipbook.EMPTY) {
            updateSimpleUV(instance);
        } else {
            instance.setUvSize(uv.flipbook.getSizeUV(instance));
            instance.getUvSize()[0] *= instance.getScaleU();
            instance.getUvSize()[1] *= instance.getScaleV();
            instance.setUvStep(uv.flipbook.getStepUV(instance));
            instance.getUvStep()[0] *= instance.getScaleU();
            instance.getUvStep()[1] *= instance.getScaleV();
            updateFlipbookUV(instance);
        }
    }

    private void doFacingCameraMode(IMolangParticleInstance instance) {
        if (faceCameraMode.isDirection()) {
            if (direction.mode == Direction.Mode.CUSTOM_DIRECTION) {
                float[] values = direction.customDirection.calculate(instance);
                instance.setXRot(values[0]);
                instance.setYRot(values[1]);
                instance.setZRot(values[2]);
            } else if (direction.minSpeedThreshold > 0.0F && Mth.lengthSquared(instance.getXd(), instance.getYd(), instance.getZd()) > instance.getPreset().minSpeedThresholdSqr) {
                instance.getFacingDirection().set(instance.getXd(), instance.getYd(), instance.getZd()).normalize();
            }
        }
    }

    private void doSize(IMolangParticleInstance instance) {
        instance.setBillboardSize(size.calculate(instance));
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    private void updateSimpleUV(IMolangParticleInstance instance) {
        TextureAtlasSprite sprite = instance.getSprite();
        if (sprite == null) return;
        float[] base = uv.uv.calculate(instance);
        float[] size = uv.uvSize.calculate(instance);
        int x = sprite.getX();
        int y = sprite.getY();
        instance.setUV(x + base[0], y + base[1], size[0] * instance.getScaleU(), size[1] * instance.getScaleV());
    }

    private void updateFlipbookUV(IMolangParticleInstance instance) {
        TextureAtlasSprite sprite = instance.getSprite();
        if (sprite == null) return;
        float[] base = uv.flipbook.baseUV.calculate(instance);
        float u = instance.getUvStep()[0] * instance.getCurrentFrame();
        float v = instance.getUvStep()[1] * instance.getCurrentFrame();
        int x = sprite.getX();
        int y = sprite.getY();
        instance.setUV(x + base[0] + u, y + base[1] + v, instance.getUvSize()[0], instance.getUvSize()[1]);
    }

    @Override
    public String toString() {
        return "ParticleAppearanceBillboard{" +
                "size=" + size +
                ", faceCameraMode=" + faceCameraMode +
                ", direction=" + direction +
                ", uv=" + uv +
                '}';
    }

    public static ParticleAppearanceBillboard fromJson(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        FloatMolangExp2 exp2 = FloatMolangExp2.fromJson(GsonHelper.getNonNull(object, "size"));
        JsonElement faceCameraMode1 = object.get("face_camera_mode");
        JsonElement faceCameraMode2 = object.get("facing_camera_mode");
        if (faceCameraMode1 == null && faceCameraMode2 == null) {
            throw new JsonParseException("Face Camera Mode not found");
        }
        if (faceCameraMode2 != null) {
            faceCameraMode1 = faceCameraMode2;
        }
        FaceCameraMode faceCameraMode3 = FaceCameraMode.fromJson(faceCameraMode1);
        JsonElement direction2 = object.get("direction");
        Direction direction1 = direction2 == null ? Direction.DEFAULT : Direction.fromJson(direction2);
        JsonElement uv2 = object.get("uv");
        UV uv1 = uv2 == null ? UV.EMPTY : UV.fromJson(uv2);
        return new ParticleAppearanceBillboard(exp2, faceCameraMode3, direction1, uv1);
    }

    public enum FaceCameraMode {
        DO_NOTHING,
        ROTATE_XYZ,
        ROTATE_Y,
        LOOKAT_XYZ,
        LOOKAT_Y,
        DIRECTION_X,
        DIRECTION_Y,
        DIRECTION_Z,
        LOOKAT_DIRECTION,
        EMITTER_TRANSFORM_XY,
        EMITTER_TRANSFORM_XZ,
        EMITTER_TRANSFORM_YZ;

        private static final Map<String, FaceCameraMode> MAP = Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(FaceCameraMode::toString, Function.identity()));

        public boolean isDirection() {
            return this == LOOKAT_DIRECTION || this == DIRECTION_X || this == DIRECTION_Y || this == DIRECTION_Z;
        }

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static FaceCameraMode fromJson(JsonElement element) {
            return MAP.getOrDefault(element.getAsString(), DO_NOTHING);
        }
    }

    public record Direction(Mode mode, float minSpeedThreshold, FloatMolangExp3 customDirection) {
        public static final Direction DEFAULT = new Direction(Mode.DERIVE_FROM_VELOCITY, 0.01F, FloatMolangExp3.ZERO);

        @Override
        public String toString() {
            return "Direction{" +
                    "mode=" + mode +
                    ", minSpeedThreshold=" + minSpeedThreshold +
                    ", customDirection=" + customDirection +
                    '}';
        }

        public static Direction fromJson(JsonElement element) {
            JsonObject object = element.getAsJsonObject();
            Mode mode1 = Mode.fromJson(GsonHelper.getNonNull(object, "mode"));
            if (mode1 == Mode.CUSTOM_DIRECTION) {
                return new Direction(Mode.CUSTOM_DIRECTION, 0, FloatMolangExp3.fromJson(object.get("custom_direction")));
            } else {
                return new Direction(Mode.DERIVE_FROM_VELOCITY, GsonHelper.getAsFloat(object, "min_speed_threshold", 0.01F), FloatMolangExp3.ZERO);
            }
        }

        public enum Mode {
            CUSTOM_DIRECTION,
            DERIVE_FROM_VELOCITY;

            @Override
            public String toString() {
                return name().toLowerCase(Locale.ROOT);
            }

            public static Mode fromJson(JsonElement element) {
                String s = element.getAsString();
                if ("custom_direction".equals(s) || "custom".equals(s)) {
                    return CUSTOM_DIRECTION;
                }
                return DERIVE_FROM_VELOCITY;
            }
        }
    }

    /// Specifies the UVs for the particle.
    ///
    /// @param texturewidth
    /// @param textureheight Specifies the assumed texture width/height, defaults to 1<p>
    ///                                                                                                          When set to 1, UV's work just like normalized UV's<p>
    ///                                                                                                          When set to the texture width/height, this works like texels
    /// @param uv
    /// @param uvSize        Assuming the specified texture width and height, use these uv coordinates.<p>
    ///                                                                                                          Evaluated every frame
    public record UV(int texturewidth, int textureheight, FloatMolangExp2 uv, FloatMolangExp2 uvSize, Flipbook flipbook) {
        public static final UV EMPTY = new UV(1, 1, FloatMolangExp2.ZERO, FloatMolangExp2.ZERO, Flipbook.EMPTY);

        @Override
        public String toString() {
            return "UV{" +
                    "texturewidth=" + texturewidth +
                    ", textureheight=" + textureheight +
                    ", uv=" + uv +
                    ", uvSize=" + uvSize +
                    ", flipbook=" + flipbook +
                    '}';
        }

        public static UV fromJson(JsonElement element) {
            JsonObject object = element.getAsJsonObject();
            JsonElement texturewidth = object.get("texturewidth");
            JsonElement texture_width = object.get("texture_width");
            if (texturewidth == null && texture_width == null) {
                throw new JsonParseException("Texture Width not found");
            }
            if (texture_width != null) {
                texturewidth = texture_width;
            }
            JsonElement textureheight = object.get("textureheight");
            JsonElement texture_height = object.get("texture_height");
            if (textureheight == null && texture_height == null) {
                throw new JsonParseException("Texture Height not found");
            }
            if (texture_height != null) {
                textureheight = texture_height;
            }
            FloatMolangExp2 exp2 = FloatMolangExp2.fromJson(object.get("uv"));
            FloatMolangExp2 exp3 = FloatMolangExp2.fromJson(object.get("uv_size"));
            JsonElement flipbook1 = object.get("flipbook");
            Flipbook flipbook2 = flipbook1 == null ? Flipbook.EMPTY : Flipbook.fromJson(flipbook1);
            return new UV(texturewidth.getAsInt(), textureheight.getAsInt(), exp2, exp3, flipbook2);
        }

        /// Alternate way via specifying a flipbook animation
        ///
        /// A flipbook animation uses pieces of the texture to animate, by stepping over time from one `frame` to another
        ///
        /// @param baseUV            Upper-left corner of starting UV patch
        /// @param sizeUV            Size of UV patch
        /// @param stepUV            How far to move the UV patch each frame
        /// @param framesPerSecond   Default frames per second
        /// @param maxFrame          Maximum frame number, with first frame being frame 1
        /// @param stretchToLifetime Optional, adjust fps to match lifetime of particle. Default=false
        /// @param loop              Optional, makes the animation loop when it reaches the end? Default=false
        public record Flipbook(FloatMolangExp2 baseUV, FloatMolangExp2 sizeUV, FloatMolangExp2 stepUV, float framesPerSecond, FloatMolangExp maxFrame, boolean stretchToLifetime, boolean loop) {
            public static final Flipbook EMPTY = new Flipbook(FloatMolangExp2.ZERO, FloatMolangExp2.ZERO, FloatMolangExp2.ZERO, 0, FloatMolangExp.ZERO, false, false);

            public float[] getSizeUV(IMolangParticleInstance instance) {
                return sizeUV.calculate(instance);
            }

            public float[] getStepUV(IMolangParticleInstance instance) {
                return stepUV.calculate(instance);
            }

            @Override
            public String toString() {
                return "Flipbook{" +
                        "baseUV=" + baseUV +
                        ", sizeUV=" + sizeUV +
                        ", stepUV=" + stepUV +
                        ", framesPerSecond=" + framesPerSecond +
                        ", maxFrame=" + maxFrame +
                        ", stretchToLifetime=" + stretchToLifetime +
                        ", loop=" + loop +
                        '}';
            }

            public static Flipbook fromJson(JsonElement element) {
                JsonObject object = element.getAsJsonObject();
                FloatMolangExp2 exp2 = FloatMolangExp2.fromJson(object.get("base_UV"));
                FloatMolangExp2 exp3 = FloatMolangExp2.fromJson(object.get("size_UV"));
                FloatMolangExp2 exp4 = FloatMolangExp2.fromJson(object.get("step_UV"));
                float second = ParticleStorm.positive(GsonHelper.getAsFloat(object, "frames_per_second", 1));
                FloatMolangExp exp = FloatMolangExp.fromJson(object.get("max_frame"));
                boolean stretchToLifetime1 = GsonHelper.getAsBoolean(object, "stretch_to_lifetime", false);
                boolean loop = GsonHelper.getAsBoolean(object, "loop", false);
                return new Flipbook(exp2, exp3, exp4, second, exp, stretchToLifetime1, loop);
            }
        }
    }
}

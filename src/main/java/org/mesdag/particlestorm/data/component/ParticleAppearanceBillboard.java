package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.DuplicateFieldDecoder;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.FloatMolangExp2;
import org.mesdag.particlestorm.data.molang.FloatMolangExp3;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.List;
import java.util.Locale;

public record ParticleAppearanceBillboard(FloatMolangExp2 size, FaceCameraMode faceCameraMode, Direction direction, UV uv) implements IParticleComponent {
    public static final ResourceLocation ID = ResourceLocation.withDefaultNamespace("particle_appearance_billboard");
    public static final Codec<ParticleAppearanceBillboard> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FloatMolangExp2.CODEC.fieldOf("size").forGetter(ParticleAppearanceBillboard::size),
            DuplicateFieldDecoder.fieldOf("face_camera_mode", "facing_camera_mode", FaceCameraMode.CODEC).forGetter(ParticleAppearanceBillboard::faceCameraMode),
            Direction.CODEC.lenientOptionalFieldOf("direction", Direction.DEFAULT).forGetter(ParticleAppearanceBillboard::direction),
            UV.CODEC.fieldOf("uv").orElse(UV.EMPTY).forGetter(ParticleAppearanceBillboard::uv)
    ).apply(instance, ParticleAppearanceBillboard::new));

    @Override
    public Codec<ParticleAppearanceBillboard> codec() {
        return CODEC;
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
            float gameTime = (float) (int) (instance.getLevel().getGameTime() & 0b11111111);
            if (gameTime % (instance.getLevel().tickRateManager().tickrate() / flipbook.framesPerSecond) < 1.0F) {
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

    public enum FaceCameraMode implements StringRepresentable {
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

        public static final Codec<FaceCameraMode> CODEC = StringRepresentable.fromEnum(FaceCameraMode::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public boolean isDirection() {
            return this == LOOKAT_DIRECTION || this == DIRECTION_X || this == DIRECTION_Y || this == DIRECTION_Z;
        }

        @Override
        public String toString() {
            return getSerializedName();
        }
    }

    public record Direction(Mode mode, float minSpeedThreshold, FloatMolangExp3 customDirection) {
        public static final Direction DEFAULT = new Direction(Direction.Mode.DERIVE_FROM_VELOCITY, 0.01F, FloatMolangExp3.ZERO);
        public static final Codec<Direction> CODEC = Mode.CODEC.dispatchMap(
                Direction::mode,
                mode -> mode == Mode.CUSTOM_DIRECTION ? RecordCodecBuilder.mapCodec(instance -> instance.group(
                        FloatMolangExp3.CODEC.fieldOf("custom_direction").orElse(FloatMolangExp3.ZERO).forGetter(Direction::customDirection)
                ).apply(instance, l -> new Direction(mode, 0, l))) : RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.FLOAT.fieldOf("min_speed_threshold").orElse(0.01F).forGetter(Direction::minSpeedThreshold)
                ).apply(instance, f -> new Direction(mode, f, FloatMolangExp3.ZERO)))
        ).codec();

        @Override
        public String toString() {
            return "Direction{" +
                    "mode=" + mode +
                    ", minSpeedThreshold=" + minSpeedThreshold +
                    ", customDirection=" + customDirection +
                    '}';
        }

        public enum Mode implements StringRepresentable {
            CUSTOM_DIRECTION,
            DERIVE_FROM_VELOCITY;

            public static final Codec<Mode> CODEC = new StringRepresentable.EnumCodec<>(values(), name -> {
                if ("custom_direction".equals(name) || "custom".equals(name)) return CUSTOM_DIRECTION;
                return DERIVE_FROM_VELOCITY;
            });

            @Override
            public String getSerializedName() {
                return name().toLowerCase(Locale.ROOT);
            }

            @Override
            public String toString() {
                return getSerializedName();
            }
        }
    }

    /**
     * Specifies the UVs for the particle.
     *
     * @param texturewidth
     * @param textureheight Specifies the assumed texture width/height, defaults to 1<p>
     *                      When set to 1, UV's work just like normalized UV's<p>
     *                      When set to the texture width/height, this works like texels
     * @param uv
     * @param uvSize        Assuming the specified texture width and height, use these uv coordinates.<p>
     *                      Evaluated every frame
     */
    public record UV(int texturewidth, int textureheight, FloatMolangExp2 uv, FloatMolangExp2 uvSize, Flipbook flipbook) {
        public static final UV EMPTY = new UV(1, 1, FloatMolangExp2.ZERO, FloatMolangExp2.ZERO, Flipbook.EMPTY);
        public static final Codec<UV> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                DuplicateFieldDecoder.fieldOf("texturewidth", "texture_width", ExtraCodecs.POSITIVE_INT).orElse(1).forGetter(UV::texturewidth),
                DuplicateFieldDecoder.fieldOf("textureheight", "texture_height", ExtraCodecs.POSITIVE_INT).orElse(1).forGetter(UV::textureheight),
                FloatMolangExp2.CODEC.fieldOf("uv").orElse(FloatMolangExp2.ZERO).forGetter(UV::uv),
                FloatMolangExp2.CODEC.fieldOf("uv_size").orElse(FloatMolangExp2.ZERO).forGetter(UV::uvSize),
                Flipbook.CODEC.fieldOf("flipbook").orElse(Flipbook.EMPTY).forGetter(UV::flipbook)
        ).apply(instance, UV::new));

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

        /// Alternate way via specifying a flipbook animation
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
            public static final Codec<Flipbook> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    FloatMolangExp2.CODEC.lenientOptionalFieldOf("base_UV", FloatMolangExp2.ZERO).forGetter(Flipbook::baseUV),
                    FloatMolangExp2.CODEC.lenientOptionalFieldOf("size_UV", FloatMolangExp2.ZERO).forGetter(Flipbook::sizeUV),
                    FloatMolangExp2.CODEC.lenientOptionalFieldOf("step_UV", FloatMolangExp2.ZERO).forGetter(Flipbook::stepUV),
                    ExtraCodecs.POSITIVE_FLOAT.lenientOptionalFieldOf("frames_per_second", 1.0F).forGetter(Flipbook::framesPerSecond),
                    FloatMolangExp.CODEC.lenientOptionalFieldOf("max_frame", FloatMolangExp.ZERO).forGetter(Flipbook::maxFrame),
                    Codec.BOOL.lenientOptionalFieldOf("stretch_to_lifetime", false).forGetter(Flipbook::stretchToLifetime),
                    Codec.BOOL.lenientOptionalFieldOf("loop", false).forGetter(Flipbook::loop)
            ).apply(instance, Flipbook::new));

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
        }
    }
}

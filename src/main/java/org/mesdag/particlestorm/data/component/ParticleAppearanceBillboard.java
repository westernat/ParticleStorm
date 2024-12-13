package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.DuplicateFieldDecoder;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.FloatMolangExp2;
import org.mesdag.particlestorm.data.molang.FloatMolangExp3;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

import java.util.List;
import java.util.Locale;

public record ParticleAppearanceBillboard(FloatMolangExp2 size, FaceCameraMode faceCameraMode, Direction direction, UV uv) implements IParticleComponent {
    public static final ResourceLocation ID = ResourceLocation.withDefaultNamespace("particle_appearance_billboard");
    public static final Codec<ParticleAppearanceBillboard> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FloatMolangExp2.CODEC.fieldOf("size").forGetter(ParticleAppearanceBillboard::size),
            DuplicateFieldDecoder.fieldOf("face_camera_mode", "facing_camera_mode", FaceCameraMode.CODEC).forGetter(ParticleAppearanceBillboard::faceCameraMode),
            Direction.CODEC.fieldOf("direction").orElseGet(() -> new Direction(Direction.Mode.DERIVE_FROM_VELOCITY, 0.01F, FloatMolangExp3.ZERO)).forGetter(ParticleAppearanceBillboard::direction),
            UV.CODEC.fieldOf("uv").orElse(UV.EMPTY).forGetter(ParticleAppearanceBillboard::uv)
    ).apply(instance, ParticleAppearanceBillboard::new));

    @Override
    public Codec<ParticleAppearanceBillboard> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(
                size.exp1(), size.exp2(), direction.customDirection.exp1(), direction.customDirection.exp2(), direction.customDirection.exp3(),
                uv.uv.exp1(), uv.uv.exp2(), uv.uvSize.exp1(), uv.uvSize.exp2(), uv.flipbook.baseUV.exp1(), uv.flipbook.baseUV.exp2(), uv.flipbook.maxFrame
        );
    }

    @Override
    public void update(MolangParticleInstance instance) {
        if (faceCameraMode.isDirection()) {
            if (direction.mode == ParticleAppearanceBillboard.Direction.Mode.CUSTOM_DIRECTION) {
                float[] values = direction.customDirection.calculate(instance);
                instance.xRot = values[0];
                instance.yRot = values[1];
                instance.setRoll(values[2]);
            } else {
                if (direction.minSpeedThreshold > 0.0F && instance.readOnlySpeed.lengthSquared() > instance.detail.minSpeedThresholdSqr) {
                    instance.facingDirection.set(instance.readOnlySpeed).normalize();
                }
            }
        }
        if (size.initialized()) {
            instance.billboardSize = size.calculate(instance);
        }
        if (uv != UV.EMPTY) {
            UV.Flipbook flipbook = uv.flipbook;
            if (flipbook == UV.Flipbook.EMPTY) {
                updateSimpleUV(instance);
            } else if (flipbook.stretchToLifetime) {
                updateFlipbookUV(instance);
                instance.maxFrame = (int) flipbook.maxFrame.calculate(instance);
                instance.currentFrame = instance.maxFrame * instance.getAge() / instance.getLifetime();
            } else if (instance.getLevel().getGameTime() % flipbook.framesPerTick < 1.0F) {
                updateFlipbookUV(instance);
                instance.maxFrame = (int) flipbook.maxFrame.calculate(instance);
                if (instance.currentFrame < instance.maxFrame) {
                    instance.currentFrame++;
                    if (flipbook.loop && instance.currentFrame == instance.maxFrame) {
                        instance.currentFrame = 0;
                    }
                } else {
                    instance.currentFrame = instance.maxFrame - 1;
                }
            }
        }
    }

    @Override
    public void apply(MolangParticleInstance instance) {
        if (faceCameraMode.isDirection()) {
            if (direction.mode == ParticleAppearanceBillboard.Direction.Mode.CUSTOM_DIRECTION) {
                float[] values = direction.customDirection.calculate(instance);
                instance.xRot = values[0];
                instance.yRot = values[1];
                instance.setRoll(values[2]);
            } else {
                double xdSqr = instance.getXd() * instance.getXd();
                double zdSqr = instance.getZd() * instance.getZd();
                if (direction.minSpeedThreshold > 0.0F && xdSqr + instance.getYd() * instance.getYd() + zdSqr > instance.detail.minSpeedThresholdSqr) {
                    instance.facingDirection.set(instance.readOnlySpeed).normalize();
                }
            }
        }
        if (size.initialized()) {
            instance.billboardSize = size.calculate(instance);
        }
        instance.UV = new float[4];
        if (uv.flipbook == UV.Flipbook.EMPTY) {
            updateSimpleUV(instance);
        } else {
            instance.uvSize = uv.flipbook.getSizeUV();
            instance.uvSize[0] *= instance.scaleU;
            instance.uvSize[1] *= instance.scaleV;
            instance.uvStep = uv.flipbook.getStepUV();
            instance.uvStep[0] *= instance.scaleU;
            instance.uvStep[1] *= instance.scaleV;
            updateFlipbookUV(instance);
        }
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    private void updateSimpleUV(MolangParticleInstance instance) {
        float[] base = uv.uv.calculate(instance);
        float[] size = uv.uvSize.calculate(instance);
        int x = instance.getSprite().getX();
        int y = instance.getSprite().getY();
        instance.setUV(x + base[0], y + base[1], size[0] * instance.scaleU, size[1] * instance.scaleV);
    }

    private void updateFlipbookUV(MolangParticleInstance instance) {
        float[] base = uv.flipbook.baseUV.calculate(instance);
        int index = instance.currentFrame;
        float u = instance.uvStep[0] * index;
        float v = instance.uvStep[1] * index;
        int x = instance.getSprite().getX();
        int y = instance.getSprite().getY();
        instance.setUV(x + base[0] + u, y + base[1] + v, instance.uvSize[0], instance.uvSize[1]);
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
        public @NotNull String getSerializedName() {
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
            public @NotNull String getSerializedName() {
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

        /**
         * Alternate way via specifying a flipbook animation<p>
         * A flipbook animation uses pieces of the texture to animate, by stepping over time from one <code>frame</code> to another
         */
        public static class Flipbook {
            public static final Flipbook EMPTY = new Flipbook(FloatMolangExp2.ZERO, List.of(), List.of(), 0, FloatMolangExp.ZERO, false, false);
            public static final Codec<Flipbook> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    FloatMolangExp2.CODEC.fieldOf("base_UV").orElse(FloatMolangExp2.ZERO).forGetter(Flipbook::baseUV),
                    Codec.list(Codec.FLOAT, 2, 2).fieldOf("size_UV").forGetter(Flipbook::sizeUV),
                    Codec.list(Codec.FLOAT, 2, 2).fieldOf("step_UV").forGetter(Flipbook::stepUV),
                    Codec.FLOAT.fieldOf("frames_per_second").orElse(1.0F).forGetter(Flipbook::framesPerSecond),
                    FloatMolangExp.CODEC.fieldOf("max_frame").orElse(FloatMolangExp.ZERO).forGetter(Flipbook::maxFrame),
                    Codec.BOOL.fieldOf("stretch_to_lifetime").orElse(false).forGetter(Flipbook::stretchToLifetime),
                    Codec.BOOL.fieldOf("loop").orElse(false).forGetter(Flipbook::loop)
            ).apply(instance, Flipbook::new));
            private final FloatMolangExp2 baseUV;
            private final List<Float> sizeUV;
            private final List<Float> stepUV;
            private final float framesPerSecond;
            private final FloatMolangExp maxFrame;
            private final boolean stretchToLifetime;
            private final boolean loop;

            private float framesPerTick = 1.0F;

            /**
             * @param baseUV            Upper-left corner of starting UV patch
             * @param sizeUV            Size of UV patch
             * @param stepUV            How far to move the UV patch each frame
             * @param framesPerSecond   Default frames per second
             * @param maxFrame          Maximum frame number, with first frame being frame 1
             * @param stretchToLifetime Optional, adjust fps to match lifetime of particle. Default=false
             * @param loop              Optional, makes the animation loop when it reaches the end? Default=false
             */
            public Flipbook(FloatMolangExp2 baseUV, List<Float> sizeUV, List<Float> stepUV, float framesPerSecond, FloatMolangExp maxFrame, boolean stretchToLifetime, boolean loop) {
                this.baseUV = baseUV;
                this.sizeUV = sizeUV;
                this.stepUV = stepUV;
                this.framesPerSecond = framesPerSecond;
                this.maxFrame = maxFrame;
                this.stretchToLifetime = stretchToLifetime;
                this.loop = loop;

                if (framesPerSecond != 0.0F) {
                    this.framesPerTick = 20.0F / framesPerSecond;
                }
            }

            public float[] getSizeUV() {
                return new float[]{sizeUV.getFirst(), sizeUV.getLast()};
            }

            public float[] getStepUV() {
                return new float[]{stepUV.getFirst(), stepUV.getLast()};
            }

            public FloatMolangExp2 baseUV() {
                return baseUV;
            }

            public List<Float> sizeUV() {
                return sizeUV;
            }

            public List<Float> stepUV() {
                return stepUV;
            }

            public float framesPerSecond() {
                return framesPerSecond;
            }

            public FloatMolangExp maxFrame() {
                return maxFrame;
            }

            public boolean stretchToLifetime() {
                return stretchToLifetime;
            }

            public boolean loop() {
                return loop;
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

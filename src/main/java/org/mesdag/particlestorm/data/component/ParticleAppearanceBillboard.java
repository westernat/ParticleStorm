package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
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
                double[] values = direction.customDirection.calculate(instance);
                instance.xRot = (float) values[0];
                instance.yRot = (float) values[1];
                instance.setRoll((float) values[2]);
            } else {
                double xdSqr = instance.getXd() * instance.getXd();
                double zdSqr = instance.getZd() * instance.getZd();
                if (direction.minSpeedThreshold > 0.0F && xdSqr + instance.getYd() * instance.getYd() + zdSqr > instance.detail.minSpeedThresholdSqr) {
                    instance.updateRotation(xdSqr, zdSqr);
                }
            }
        }
        if (size.initialized()) {
            instance.billboardSize = size.calculate(instance);
        }
        if (uv != UV.EMPTY) {
            if (uv.flipbook != UV.Flipbook.EMPTY) {
                // todo 计时
                instance.baseUV = uv.flipbook.baseUV.calculate(instance);
            }
        }
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    public enum FaceCameraMode implements StringRepresentable {
        ROTATE_XYZ,
        ROTATE_Y,
        LOOKAT_XYZ,
        LOOKAT_Y,
        DIRECTION_X,
        DIRECTION_Y,
        DIRECTION_Z,
        EMITTER_TRANSFORM_XY,
        EMITTER_TRANSFORM_XZ,
        EMITTER_TRANSFORM_YZ;

        public static final Codec<FaceCameraMode> CODEC = StringRepresentable.fromEnum(FaceCameraMode::values);

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public boolean isDirection() {
            return this == DIRECTION_X || this == DIRECTION_Y || this == DIRECTION_Z;
        }
    }

    public record Direction(Mode mode, float minSpeedThreshold, FloatMolangExp3 customDirection) {
        public static final Codec<Direction> CODEC = Mode.CODEC.dispatchMap(
                Direction::mode,
                mode -> mode == Mode.CUSTOM_DIRECTION ? RecordCodecBuilder.mapCodec(instance -> instance.group(
                        FloatMolangExp3.CODEC.fieldOf("custom_direction").orElse(FloatMolangExp3.ZERO).forGetter(Direction::customDirection)
                ).apply(instance, l -> new Direction(mode, 0, l))) : RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.FLOAT.fieldOf("min_speed_threshold").forGetter(Direction::minSpeedThreshold)
                ).apply(instance, f -> new Direction(mode, f, FloatMolangExp3.ZERO)))
        ).codec();

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
        }
    }

    public record UV(int texturewidth, int textureheight, FloatMolangExp2 uv, FloatMolangExp2 uvSize, Flipbook flipbook) {
        public static final UV EMPTY = new UV(0, 0, FloatMolangExp2.ZERO, FloatMolangExp2.ZERO, Flipbook.EMPTY);
        public static final Codec<UV> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                DuplicateFieldDecoder.fieldOf("texturewidth", "texture_width", ExtraCodecs.POSITIVE_INT).forGetter(UV::texturewidth),
                DuplicateFieldDecoder.fieldOf("textureheight", "texture_height", ExtraCodecs.POSITIVE_INT).forGetter(UV::textureheight),
                FloatMolangExp2.CODEC.fieldOf("uv").orElse(FloatMolangExp2.ZERO).forGetter(UV::uv),
                FloatMolangExp2.CODEC.fieldOf("uv_size").orElse(FloatMolangExp2.ZERO).forGetter(UV::uvSize),
                Flipbook.CODEC.fieldOf("flipbook").orElse(Flipbook.EMPTY).forGetter(UV::flipbook)
        ).apply(instance, UV::new));

        public record Flipbook(FloatMolangExp2 baseUV, List<Float> sizeUV, List<Float> stepUV, float framesPerSecond, FloatMolangExp maxFrame, boolean stretchToLifetime, boolean loop) {
            public static final Flipbook EMPTY = new Flipbook(FloatMolangExp2.ZERO, List.of(), List.of(), 0, FloatMolangExp.ZERO, false, false);
            public static final Codec<Flipbook> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    FloatMolangExp2.CODEC.fieldOf("base_UV").orElse(FloatMolangExp2.ZERO).forGetter(Flipbook::baseUV),
                    Codec.list(Codec.FLOAT, 2, 2).fieldOf("size_UV").forGetter(Flipbook::sizeUV),
                    Codec.list(Codec.FLOAT, 2, 2).fieldOf("step_UV").forGetter(Flipbook::stepUV),
                    Codec.FLOAT.fieldOf("frames_per_second").forGetter(Flipbook::framesPerSecond),
                    FloatMolangExp.CODEC.fieldOf("max_frame").orElse(FloatMolangExp.ZERO).forGetter(Flipbook::maxFrame),
                    Codec.BOOL.fieldOf("stretch_to_lifetime").orElse(false).forGetter(Flipbook::stretchToLifetime),
                    Codec.BOOL.fieldOf("loop").orElse(false).forGetter(Flipbook::loop)
            ).apply(instance, Flipbook::new));
        }
    }
}

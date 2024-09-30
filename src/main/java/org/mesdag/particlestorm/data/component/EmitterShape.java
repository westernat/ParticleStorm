package org.mesdag.particlestorm.data.component;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.FloatMolangExp3;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.mixin.ParticleEngineAccessor;
import org.mesdag.particlestorm.particle.MolangParticleInstance;
import org.mesdag.particlestorm.particle.ParticleEmitterEntity;

import java.util.Arrays;
import java.util.List;

public abstract class EmitterShape implements IEmitterComponent {
    protected final boolean surfaceOnly;

    protected EmitterShape(boolean surfaceOnly) {
        this.surfaceOnly = surfaceOnly;
    }

    /**
     * Emit only from the edge of the shape
     */
    public boolean isSurfaceOnly() {
        return surfaceOnly;
    }

    private static void emittingParticle(ParticleEmitterEntity entity, Vector3f position, Vector3f speed) {
        Particle particle = Minecraft.getInstance().particleEngine.createParticle(entity.getDetail().option, position.x, position.y, position.z, speed.x, speed.y, speed.z);
        if (particle instanceof MolangParticleInstance instance) {
            instance.getVariableTable().subTable = entity.getVariableTable();

            instance.detail.assignments.forEach(assignment -> {
                // 重定向，防止污染变量表
                instance.getVariableTable().setValue(assignment.variable().name(), assignment.variable());
            });
            instance.components = instance.detail.effect.components.values().stream().filter(c -> {
                if (c instanceof IParticleComponent p) {
                    p.apply(instance);
                    return p.requireUpdate();
                }
                return false;
            }).map(c -> (IParticleComponent) c).toList();
        }
    }

    private static boolean hasSpaceInParticleLimit(ParticleEmitterEntity entity) {
        ParticleGroup particleGroup = entity.particleGroup;
        return ((ParticleEngineAccessor) Minecraft.getInstance().particleEngine).trackedParticleCounts().getInt(particleGroup) < particleGroup.getLimit();
    }

    /**
     * This component spawns particles using a disc shape, particles can be spawned inside the shape or on its outer perimeter.
     */
    public static final class Disc extends EmitterShape {
        public static final Codec<Disc> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FloatMolangExp3.CODEC.fieldOf("offset").orElseGet(() -> FloatMolangExp3.ZERO).forGetter(Disc::getOffset),
                FloatMolangExp.CODEC.fieldOf("radius").orElse(FloatMolangExp.ONE).forGetter(Disc::getRadius),
                PlaneNormal.CODEC.fieldOf("plane_normal").orElse(PlaneNormal.Y).forGetter(Disc::getPlaneNormal),
                Direction.CODEC.fieldOf("direction").orElse(Direction.OUTWARDS).forGetter(Disc::getDirection),
                Codec.BOOL.fieldOf("surface_only").orElse(false).forGetter(EmitterShape::isSurfaceOnly)
        ).apply(instance, Disc::new));
        private final FloatMolangExp3 offset;
        private final FloatMolangExp radius;
        private final PlaneNormal planeNormal;
        private final Direction direction;

        private static final Vector3f EL = new Vector3f();

        public Disc(FloatMolangExp3 offset, FloatMolangExp radius, PlaneNormal planeNormal, Direction direction, boolean surfaceOnly) {
            super(surfaceOnly);
            this.offset = offset;
            this.radius = radius;
            this.planeNormal = planeNormal;
            this.direction = direction;
        }

        /**
         * Specifies the offset from the emitter to emit the particles
         * <p>
         * Evaluated once per particle emitted
         */
        public FloatMolangExp3 getOffset() {
            return offset;
        }

        /**
         * Disc radius
         * <p>
         * Evaluated once per particle emitted
         */
        public FloatMolangExp getRadius() {
            return radius;
        }

        /**
         * Specifies the normal of the disc plane, the disc will be perpendicular to this direction
         */
        public PlaneNormal getPlaneNormal() {
            return planeNormal;
        }

        /**
         * Specifies the direction of particles.
         */
        public Direction getDirection() {
            return direction;
        }

        @Override
        public Codec<Disc> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(
                    offset.exp1(), offset.exp2(), offset.exp3(), radius,
                    planeNormal.plane.exp1(), planeNormal.plane.exp2(), planeNormal.plane.exp3(),
                    direction.direct.exp1(), direction.direct.exp2(), direction.direct.exp3()
            );
        }

        @Override
        public void update(ParticleEmitterEntity entity) {
            if (entity.spawned) return;
            for (int num = 0; num < entity.spawnRate; num++) {
                if (hasSpaceInParticleLimit(entity)) {
                    float[] off = offset.calculate(entity);
                    Vector3f position = entity.position().toVector3f().add(off[0], off[1], off[2]);
                    Vector3f speed = new Vector3f();
                    float radius = this.radius.calculate(entity);
                    float op = entity.level().random.nextFloat() * Mth.TWO_PI;
                    float sp = surfaceOnly ? radius : radius * Mth.sqrt(entity.level().random.nextFloat());
                    position.x += sp * Mth.cos(op);
                    position.z += sp * Mth.sin(op);
                    float[] lp = planeNormal.getPlane().calculate(entity);
                    if (!Arrays.equals(lp, PlaneNormal.FN)) {
                        Quaternionf quaternion = new Quaternionf();
                        float dot = new Vector3f(PlaneNormal.FY).dot(new Vector3f(lp)) + 1;
                        if (dot < 0) {
                            quaternion.x = 0;
                            quaternion.y = 0;
                            quaternion.z = 1;
                            quaternion.w = 0;
                        } else {
                            quaternion.x = lp[2];
                            quaternion.y = 0;
                            quaternion.z = -lp[0];
                            quaternion.w = dot;
                        }
                        quaternion.normalize();

                        float t = position.x, n = position.y, r = position.z;
                        float i = quaternion.x;
                        float a = quaternion.y;
                        float o = quaternion.z;
                        float s = quaternion.w;
                        float l = s * t + a * r - o * n;
                        float c = s * n + o * t - i * r;
                        float u = s * r + i * n - a * t;
                        float h = -i * t - a * n - o * r;
                        position.x = l * s + h * -i + c * -o - u * -a;
                        position.y = c * s + h * -a + u * -i - l * -o;
                        position.z = u * s + h * -o + l * -a - c * -i;
                    }
                    direction.apply(entity, this, position, speed);
                    emittingParticle(entity, position, speed);
                }
            }
            entity.spawned = true;
        }

        @Override
        public boolean requireUpdate() {
            return true;
        }

        /**
         * Custom direction for the normal
         */
        public static class PlaneNormal {
            public static final PlaneNormal X = new PlaneNormal("x", FloatMolangExp3.X);
            public static final PlaneNormal Y = new PlaneNormal("y", FloatMolangExp3.Y);
            public static final PlaneNormal Z = new PlaneNormal("z", FloatMolangExp3.Z);
            public static final float[] FN = new float[]{0.0F, 0.0F, 0.0F};
            public static final float[] FX = new float[]{1.0F, 0.0F, 0.0F};
            public static final float[] FY = new float[]{0.0F, 1.0F, 0.0F};
            public static final float[] FZ = new float[]{0.0F, 0.0F, 1.0F};
            public static final Codec<PlaneNormal> CODEC = Codec.either(Codec.STRING, FloatMolangExp3.CODEC).xmap(
                    either -> either.map(d -> switch (d) {
                        case "x" -> X;
                        case "z" -> Z;
                        default -> Y;
                    }, list -> new PlaneNormal("custom", list)),
                    plane -> Either.right(plane.plane)
            );
            private final String name;
            private final FloatMolangExp3 plane;

            PlaneNormal(String name, FloatMolangExp3 list) {
                this.name = name;
                this.plane = list;
            }

            public FloatMolangExp3 getPlane() {
                return plane;
            }

            public String getName() {
                return name;
            }

            public boolean isCustom() {
                return "custom".equals(name);
            }
        }
    }

    /**
     * All particles come out of a box of the specified size from the emitter.
     */
    public static final class Box extends EmitterShape {
        public static final Codec<Box> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Direction.CODEC.fieldOf("direction").orElse(Direction.OUTWARDS).forGetter(Box::getDirection),
                Codec.BOOL.fieldOf("surface_only").orElse(false).forGetter(EmitterShape::isSurfaceOnly)
        ).apply(instance, Box::new));
        private final Direction direction;

        public Box(Direction direction, boolean surfaceOnly) {
            super(surfaceOnly);
            this.direction = direction;
        }

        /**
         * Specifies the direction of particles.
         */
        public Direction getDirection() {
            return direction;
        }

        @Override
        public Codec<Box> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(direction.direct.exp1(), direction.direct.exp2(), direction.direct.exp3());
        }
    }

    /**
     * All particles are emitted based on a specified set of Molang expressions.
     */
    public static final class Custom extends EmitterShape {
        public static final Codec<Custom> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FloatMolangExp3.CODEC.fieldOf("offset").orElse(FloatMolangExp3.ZERO).forGetter(Custom::getOffset),
                FloatMolangExp3.CODEC.fieldOf("direction").orElse(FloatMolangExp3.ZERO).forGetter(Custom::getDirection)
        ).apply(instance, Custom::new));
        private final FloatMolangExp3 offset;
        private final FloatMolangExp3 direction;

        public Custom(FloatMolangExp3 offset, FloatMolangExp3 direction) {
            super(false);
            this.offset = offset;
            this.direction = direction;
        }

        public FloatMolangExp3 getOffset() {
            return offset;
        }

        public FloatMolangExp3 getDirection() {
            return direction;
        }

        @Override
        public Codec<Custom> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(offset.exp1(), offset.exp2(), offset.exp3(), direction.exp1(), direction.exp2(), direction.exp3());
        }
    }

    /**
     * All particles come out of the axis-aligned bounding box (AABB) for the entity the emitter is attached to, or the emitter point if no entity.
     */
    public static final class EntityAABB extends EmitterShape {
        public static final Codec<EntityAABB> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Direction.CODEC.fieldOf("direction").orElse(Direction.OUTWARDS).forGetter(EntityAABB::getDirection),
                Codec.BOOL.fieldOf("surface_only").orElse(false).forGetter(EmitterShape::isSurfaceOnly)
        ).apply(instance, EntityAABB::new));
        private final Direction direction;

        public EntityAABB(Direction direction, boolean surfaceOnly) {
            super(surfaceOnly);
            this.direction = direction;
        }

        public Direction getDirection() {
            return direction;
        }

        @Override
        public Codec<? extends IComponent> codec() {
            return null;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(direction.direct.exp1(), direction.direct.exp2(), direction.direct.exp3());
        }
    }

    /**
     * All particles come out of a point offset from the emitter.
     */
    public static final class Point extends EmitterShape {
        public static final Codec<Point> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FloatMolangExp3.CODEC.fieldOf("offset").orElse(FloatMolangExp3.ZERO).forGetter(Point::getOffset),
                FloatMolangExp3.CODEC.fieldOf("direction").orElse(FloatMolangExp3.ZERO).forGetter(Point::getDirection)
        ).apply(instance, Point::new));
        private final FloatMolangExp3 offset;
        private final FloatMolangExp3 direction;

        public Point(FloatMolangExp3 offset, FloatMolangExp3 direction) {
            super(false);
            this.offset = offset;
            this.direction = direction;
        }

        /**
         * Specifies the offset from the emitter to emit the particles
         * <p>
         * Evaluated once per particle emitted
         */
        public FloatMolangExp3 getOffset() {
            return offset;
        }

        /**
         * Specifies the direction of particles.
         */
        public FloatMolangExp3 getDirection() {
            return direction;
        }

        @Override
        public Codec<Point> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(offset.exp1(), offset.exp2(), offset.exp3(), direction.exp1(), direction.exp2(), direction.exp3());
        }
    }

    public static final class Sphere extends EmitterShape {
        public static final Codec<Sphere> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FloatMolangExp3.CODEC.fieldOf("offset").orElse(FloatMolangExp3.ZERO).forGetter(Sphere::getOffset),
                FloatMolangExp.CODEC.fieldOf("radius").orElse(FloatMolangExp.ONE).forGetter(Sphere::getRadius),
                Direction.CODEC.fieldOf("direction").orElse(Direction.OUTWARDS).forGetter(Sphere::getDirection),
                Codec.BOOL.fieldOf("surface_only").orElse(false).forGetter(EmitterShape::isSurfaceOnly)
        ).apply(instance, Sphere::new));

        private final FloatMolangExp3 offset;
        private final FloatMolangExp radius;
        private final Direction direction;

        public Sphere(FloatMolangExp3 offset, FloatMolangExp radius, Direction direction, boolean surfaceOnly) {
            super(surfaceOnly);
            this.offset = offset;
            this.radius = radius;
            this.direction = direction;
        }

        /**
         * Specifies the offset from the emitter to emit the particles
         * <p>
         * Evaluated once per particle emitted
         */
        public FloatMolangExp3 getOffset() {
            return offset;
        }

        /**
         * Sphere radius
         * <p>
         * Evaluated once per particle emitted
         */
        public FloatMolangExp getRadius() {
            return radius;
        }

        /**
         * Specifies the direction of particles.
         */
        public Direction getDirection() {
            return direction;
        }

        @Override
        public Codec<Sphere> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(
                    offset.exp1(), offset.exp2(), offset.exp3(), radius,
                    direction.direct.exp1(), direction.direct.exp2(), direction.direct.exp3()
            );
        }
    }

    /**
     * Evaluated once per particle emitted
     */
    public static class Direction implements StringRepresentable {
        /**
         * Particle direction towards center of shape
         */
        public static final Direction INWARDS = new Direction("inwards", FloatMolangExp3.ZERO);
        /**
         * Particle direction away from center of shape
         */
        public static final Direction OUTWARDS = new Direction("outwards", FloatMolangExp3.ZERO);
        public static final Codec<Direction> DIRECTION_CODEC = StringRepresentable.fromValues(() -> new Direction[]{INWARDS, OUTWARDS});
        public static final Codec<Direction> CODEC = Codec.either(DIRECTION_CODEC, FloatMolangExp3.CODEC).xmap(
                either -> either.map(dir -> dir, list -> new Direction("custom", list)),
                dir -> dir.direct == FloatMolangExp3.ZERO ? Either.left(dir) : Either.right(dir.direct)
        );
        private final String name;
        private final FloatMolangExp3 direct;

        private static final Quaternionf HD = new Quaternionf();
        private static final Vector3f OX = new Vector3f();

        Direction(String name, FloatMolangExp3 direct) {
            this.name = name;
            this.direct = direct;
        }

        public FloatMolangExp3 getDirect() {
            return direct;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        public void apply(ParticleEmitterEntity entity, EmitterShape shape, Vector3f position, Vector3f speed) {
            // todo inherited_particle_speed
            if (this == INWARDS || this == OUTWARDS) {
                if (shape instanceof Point) {
                    applyEuler(getRandomEuler(entity.level().random), speed.set(1, 0, 0));
                } else {
                    speed.set(position).normalize();
                    if (this == INWARDS) speed.negate();
                }
            } else { // custom
                speed.set(new Vector3f(direct.calculate(entity)).normalize());
            }
        }

        private Vector3f getRandomEuler(RandomSource random) {
            float x = random.nextFloat() * (random.nextBoolean() ? Mth.PI : -Mth.PI);
            float y = random.nextFloat() * (random.nextBoolean() ? Mth.PI : -Mth.PI);
            float z = random.nextFloat() * (random.nextBoolean() ? Mth.PI : -Mth.PI);
            return OX.set(x, y, z);
        }

        private void applyEuler(Vector3f euler, Vector3f dest) {
            HD.rotateXYZ(euler.x, euler.y, euler.z).transform(dest);
        }
    }
}

package org.mesdag.particlestorm.data.component;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.FloatMolangExp3;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.List;
import java.util.Locale;

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

        /**
         * Custom direction for the normal
         */
        public static class PlaneNormal {
            /**
             * This variant has the normal in the x-axis
             */
            public static final PlaneNormal X = new PlaneNormal("x", FloatMolangExp3.X);
            /**
             * This variant has the normal in the y-axis
             */
            public static final PlaneNormal Y = new PlaneNormal("y", FloatMolangExp3.Y);
            /**
             * This variant has the normal in the z-axis
             */
            public static final PlaneNormal Z = new PlaneNormal("z", FloatMolangExp3.Z);
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

        Direction(String name, FloatMolangExp3 direct) {
            this.name = name;
            this.direct = direct;
        }

        public FloatMolangExp3 getDirect() {
            return direct;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name.toLowerCase(Locale.ROOT);
        }
    }
}

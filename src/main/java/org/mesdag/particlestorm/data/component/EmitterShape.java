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
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.mesdag.particlestorm.data.MathHelper;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.FloatMolangExp3;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.mixin.ParticleEngineAccessor;
import org.mesdag.particlestorm.particle.MolangParticleInstance;
import org.mesdag.particlestorm.particle.ParticleDetail;
import org.mesdag.particlestorm.particle.ParticleEmitter;

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

    @Override
    public void update(ParticleEmitter entity) {
        if (entity.spawned) return;
        if (entity.spawnDuration <= 1 || entity.age % entity.spawnDuration == 0) {
            for (int num = 0; num < entity.spawnRate; num++) {
                if (hasSpaceInParticleLimit(entity)) {
                    emittingParticle(entity);
                }
            }
            if (entity.getDetail().emitterRateType == EmitterRate.Type.INSTANT) {
                entity.spawned = true;
            }
        }
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    protected abstract void initializeParticle(MolangInstance instance, Vector3f position, Vector3f speed);

    private void emittingParticle(ParticleEmitter emitter) {
        Particle particle = ((ParticleEngineAccessor) Minecraft.getInstance().particleEngine).callMakeParticle(emitter.getDetail().option, emitter.getX(), emitter.getY(), emitter.getZ(), 0.0, 0.0, 0.0);
        if (particle instanceof MolangParticleInstance instance) {
            instance.emitter = emitter;
            instance.getVariableTable().subTable = emitter.getVariableTable();

            Vector3f position = new Vector3f();
            Vector3f speed = new Vector3f();
            initializeParticle(instance, position, speed);
            speed.mul(emitter.particleInitialSpeed);
            if (emitter.parentMode == ParticleEmitter.ParentMode.LOCATOR) {
                position.x *= -1;
                position.y *= -1;
                speed.x *= -1;
                speed.y *= -1;
            }
            if (emitter.parentMode != ParticleEmitter.ParentMode.WORLD && emitter.getDetail().localPosition && !emitter.getDetail().localRotation) {
                speed.x *= -1;
                speed.z *= -1;
            }
            if (emitter.getDetail().localRotation) {
                MathHelper.applyEuler(emitter.rot.x, emitter.rot.y, 0.0F, position);
            }
            if (emitter.getDetail().localPosition) {
                Vec3 emitterPos = emitter.getPosition();
                position.add((float) emitterPos.x, (float) emitterPos.y, (float) emitterPos.z);
            }
            if (emitter.getDetail().localVelocity) {
                Vec3 emitterVec = emitter.deltaMovement;
                speed.add((float) emitterVec.x, (float) emitterVec.y, (float) emitterVec.z);
            }
            speed.mul(emitter.invTickRate);

            instance.setParticleSpeed(speed.x, speed.y, speed.z);
            instance.setPos(position.x, position.y, position.z);
            instance.setPosO(position.x, position.y, position.z);
            instance.particleGroup = emitter.particleGroup;
            ParticleDetail detail = instance.detail;
            detail.assignments.forEach(assignment -> {
                // 重定向，防止污染变量表
                String name = assignment.variable().name();
                instance.getVariableTable().setValue(name, new Variable(name, assignment.value()));
            });
            for (IParticleComponent component : detail.effect.orderedParticleComponents) {
                component.apply(instance);
            }
            instance.components = detail.effect.orderedParticleComponentsWhichRequireUpdate;
            instance.motionDynamic = detail.motionDynamic;
            if (!instance.motionDynamic) instance.setParticleSpeed(0.0, 0.0, 0.0);
        }
        Minecraft.getInstance().particleEngine.add(particle);
    }

    private static boolean hasSpaceInParticleLimit(ParticleEmitter emitter) {
        ParticleGroup particleGroup = emitter.particleGroup;
        return ((ParticleEngineAccessor) Minecraft.getInstance().particleEngine).trackedParticleCounts().getInt(particleGroup) < particleGroup.getLimit();
    }

    /**
     * This component spawns particles using a disc shape, particles can be spawned inside the shape or on its outer perimeter.
     */
    public static final class Disc extends EmitterShape {
        public static final Codec<Disc> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FloatMolangExp3.CODEC.fieldOf("offset").orElseGet(() -> FloatMolangExp3.ZERO).forGetter(disc -> disc.offset),
                FloatMolangExp.CODEC.fieldOf("radius").orElse(FloatMolangExp.ONE).forGetter(disc -> disc.radius),
                PlaneNormal.CODEC.fieldOf("plane_normal").orElse(PlaneNormal.Y).forGetter(disc -> disc.planeNormal),
                Direction.CODEC.fieldOf("direction").orElse(Direction.OUTWARDS).forGetter(disc -> disc.direction),
                Codec.BOOL.fieldOf("surface_only").orElse(false).forGetter(EmitterShape::isSurfaceOnly)
        ).apply(instance, Disc::new));
        /**
         * Specifies the offset from the emitter to emit the particles
         * <p>
         * Evaluated once per particle emitted
         */
        public final FloatMolangExp3 offset;
        /**
         * Disc radius
         * <p>
         * Evaluated once per particle emitted
         */
        public final FloatMolangExp radius;
        /**
         * Specifies the normal of the disc plane, the disc will be perpendicular to this direction
         */
        public final PlaneNormal planeNormal;
        /**
         * Specifies the direction of particles.
         */
        public final Direction direction;

        public Disc(FloatMolangExp3 offset, FloatMolangExp radius, PlaneNormal planeNormal, Direction direction, boolean surfaceOnly) {
            super(surfaceOnly);
            this.offset = offset;
            this.radius = radius;
            this.planeNormal = planeNormal;
            this.direction = direction;
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
        protected void initializeParticle(MolangInstance instance, Vector3f position, Vector3f speed) {
            position.set(offset.calculate(instance));
            float radius = this.radius.calculate(instance);
            float op = instance.getLevel().random.nextFloat() * Mth.TWO_PI;
            float sp = surfaceOnly ? radius : radius * Mth.sqrt(instance.getLevel().random.nextFloat());
            position.x += sp * Mth.cos(op);
            position.z += sp * Mth.sin(op);
            float[] lp = planeNormal.plane.calculate(instance);
            if (!Arrays.equals(lp, PlaneNormal.FN)) {
                Quaternionf quaternion = MathHelper.setFromUnitVectors(PlaneNormal.VY, new Vector3f(lp), new Quaternionf());
                MathHelper.applyQuaternion(quaternion, position);
            }
            direction.apply(instance, this, position, speed);
        }

        @Override
        public String toString() {
            return "Disc{" +
                    "offset=" + offset +
                    ", radius=" + radius +
                    ", planeNormal=" + planeNormal +
                    ", direction=" + direction +
                    ", surfaceOnly=" + surfaceOnly +
                    '}';
        }

        /**
         * Custom direction for the normal
         */
        public static class PlaneNormal {
            public static final PlaneNormal X = new PlaneNormal("x", FloatMolangExp3.X);
            public static final PlaneNormal Y = new PlaneNormal("y", FloatMolangExp3.Y);
            public static final PlaneNormal Z = new PlaneNormal("z", FloatMolangExp3.Z);
            public static final float[] FN = new float[]{0.0F, 0.0F, 0.0F};
            public static final Vector3f VY = new Vector3f(0.0F, 1.0F, 0.0F);
            public static final Codec<PlaneNormal> CODEC = Codec.either(Codec.STRING, FloatMolangExp3.CODEC).xmap(
                    either -> either.map(d -> switch (d) {
                        case "x" -> X;
                        case "z" -> Z;
                        default -> Y;
                    }, list -> new PlaneNormal("custom", list)),
                    plane -> Either.right(plane.plane)
            );
            public final String name;
            public final FloatMolangExp3 plane;

            PlaneNormal(String name, FloatMolangExp3 list) {
                this.name = name;
                this.plane = list;
            }

            public boolean isCustom() {
                return "custom".equals(name);
            }

            @Override
            public String toString() {
                return "PlaneNormal{" +
                        "name='" + name + '\'' +
                        ", plane=" + plane +
                        '}';
            }
        }
    }

    /**
     * All particles come out of a box of the specified size from the emitter.
     */
    public static final class Box extends EmitterShape {
        public static final Codec<Box> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FloatMolangExp3.CODEC.fieldOf("offset").orElse(FloatMolangExp3.ZERO).forGetter(box -> box.offset),
                FloatMolangExp3.CODEC.fieldOf("half_dimensions").orElse(FloatMolangExp3.ZERO).forGetter(box -> box.halfDimensions),
                Direction.CODEC.fieldOf("direction").orElse(Direction.OUTWARDS).forGetter(box -> box.direction),
                Codec.BOOL.fieldOf("surface_only").orElse(false).forGetter(EmitterShape::isSurfaceOnly)
        ).apply(instance, Box::new));
        /**
         * Specifies the offset from the emitter to emit the particles<p>
         * Evaluated once per particle emitted
         */
        public final FloatMolangExp3 offset;
        public final FloatMolangExp3 halfDimensions;
        /**
         * Specifies the direction of particles.
         */
        public final Direction direction;

        public Box(FloatMolangExp3 offset, FloatMolangExp3 halfDimensions, Direction direction, boolean surfaceOnly) {
            super(surfaceOnly);
            this.offset = offset;
            this.halfDimensions = halfDimensions;
            this.direction = direction;
        }

        @Override
        public Codec<Box> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(direction.direct.exp1(), direction.direct.exp2(), direction.direct.exp3());
        }

        @Override
        protected void initializeParticle(MolangInstance instance, Vector3f position, Vector3f speed) {
            position.set(offset.calculate(instance));
            float[] n = halfDimensions.calculate(instance);
            RandomSource random = instance.getLevel().random;
            position.x += MathHelper.nextFloat(random, -n[0], n[0]);
            position.y += MathHelper.nextFloat(random, -n[1], n[1]);
            position.z += MathHelper.nextFloat(random, -n[2], n[2]);
            if (surfaceOnly) {
                int r = random.nextInt(0, 3);
                boolean i = random.nextBoolean();
                position.setComponent(r, n[r] * (i ? 1 : -1));
            }
            direction.apply(instance, this, position, speed);
        }

        @Override
        public String toString() {
            return "Box{" +
                    "surfaceOnly=" + surfaceOnly +
                    ", direction=" + direction +
                    ", halfDimensions=" + halfDimensions +
                    ", offset=" + offset +
                    '}';
        }
    }

    /**
     * All particles come out of the axis-aligned bounding box (AABB) for the entity the emitter is attached to, or the emitter point if no entity.
     */
    public static final class EntityAABB extends EmitterShape {
        public static final Codec<EntityAABB> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Direction.CODEC.fieldOf("direction").orElse(Direction.OUTWARDS).forGetter(entityAABB -> entityAABB.direction),
                Codec.BOOL.fieldOf("surface_only").orElse(false).forGetter(EmitterShape::isSurfaceOnly)
        ).apply(instance, EntityAABB::new));
        public final Direction direction;

        public EntityAABB(Direction direction, boolean surfaceOnly) {
            super(surfaceOnly);
            this.direction = direction;
        }

        @Override
        public Codec<EntityAABB> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(direction.direct.exp1(), direction.direct.exp2(), direction.direct.exp3());
        }

        @Override
        protected void initializeParticle(MolangInstance instance, Vector3f position, Vector3f speed) {
            EntityDimensions dimensions = instance.getAttachedEntity().getDimensions(instance.getAttachedEntity().getPose());
            Vector3f n = new Vector3f(dimensions.width(), dimensions.height(), dimensions.width());
            RandomSource random = instance.getLevel().random;
            position.x = MathHelper.nextFloat(random, -n.x, n.x);
            position.y = MathHelper.nextFloat(random, -n.y, n.y);
            position.z = MathHelper.nextFloat(random, -n.z, n.z);
            if (surfaceOnly) {
                int r = random.nextInt(0, 3);
                boolean i = random.nextBoolean();
                position.setComponent(r, n.get(r) * (i ? 1 : -1));
            }
            direction.apply(instance, this, position, speed);
        }

        @Override
        public String toString() {
            return "EntityAABB{" +
                    "direction=" + direction +
                    ", surfaceOnly=" + surfaceOnly +
                    '}';
        }
    }

    /**
     * All particles come out of a point offset from the emitter.
     */
    public static final class Point extends EmitterShape {
        public static final Codec<Point> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FloatMolangExp3.CODEC.fieldOf("offset").orElse(FloatMolangExp3.ZERO).forGetter(point -> point.offset),
                Direction.CODEC.fieldOf("direction").orElse(Direction.OUTWARDS).forGetter(point -> point.direction)
        ).apply(instance, Point::new));
        /**
         * Specifies the offset from the emitter to emit the particles
         * <p>
         * Evaluated once per particle emitted
         */
        public final FloatMolangExp3 offset;
        /**
         * Specifies the direction of particles.
         */
        public final Direction direction;

        public Point(FloatMolangExp3 offset, Direction direction) {
            super(false);
            this.offset = offset;
            this.direction = direction;
        }

        @Override
        public Codec<Point> codec() {
            return CODEC;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(
                    offset.exp1(), offset.exp2(), offset.exp3(),
                    direction.direct.exp1(), direction.direct.exp2(), direction.direct.exp3()
            );
        }

        @Override
        protected void initializeParticle(MolangInstance instance, Vector3f position, Vector3f speed) {
            position.set(offset.calculate(instance));
            direction.apply(instance, this, position, speed);
        }

        @Override
        public String toString() {
            return "Point{" +
                    "surfaceOnly=" + surfaceOnly +
                    ", direction=" + direction +
                    ", offset=" + offset +
                    '}';
        }
    }

    public static final class Sphere extends EmitterShape {
        public static final Codec<Sphere> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FloatMolangExp3.CODEC.fieldOf("offset").orElse(FloatMolangExp3.ZERO).forGetter(sphere -> sphere.offset),
                FloatMolangExp.CODEC.fieldOf("radius").orElse(FloatMolangExp.ONE).forGetter(sphere -> sphere.radius),
                Direction.CODEC.fieldOf("direction").orElse(Direction.OUTWARDS).forGetter(sphere -> sphere.direction),
                Codec.BOOL.fieldOf("surface_only").orElse(false).forGetter(EmitterShape::isSurfaceOnly)
        ).apply(instance, Sphere::new));
        /**
         * Specifies the offset from the emitter to emit the particles
         * <p>
         * Evaluated once per particle emitted
         */
        public final FloatMolangExp3 offset;
        /**
         * Sphere radius
         * <p>
         * Evaluated once per particle emitted
         */
        public final FloatMolangExp radius;
        /**
         * Specifies the direction of particles.
         */
        public final Direction direction;

        public Sphere(FloatMolangExp3 offset, FloatMolangExp radius, Direction direction, boolean surfaceOnly) {
            super(surfaceOnly);
            this.offset = offset;
            this.radius = radius;
            this.direction = direction;
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

        @Override
        protected void initializeParticle(MolangInstance invTickRate, Vector3f position, Vector3f speed) {
            position.set(offset.calculate(invTickRate));
            float a = radius.calculate(invTickRate);
            position.x = surfaceOnly ? a : a * invTickRate.getLevel().random.nextFloat();
            MathHelper.applyEuler(MathHelper.getRandomEuler(invTickRate.getLevel().random), position);
            direction.apply(invTickRate, this, position, speed);
        }

        @Override
        public String toString() {
            return "Sphere{" +
                    "offset=" + offset +
                    ", radius=" + radius +
                    ", direction=" + direction +
                    ", surfaceOnly=" + surfaceOnly +
                    '}';
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
        public final String name;
        public final FloatMolangExp3 direct;

        Direction(String name, FloatMolangExp3 direct) {
            this.name = name;
            this.direct = direct;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        public void apply(MolangInstance instance, EmitterShape shape, Vector3f position, Vector3f speed) {
            // todo inherited_particle_speed
            if (this == INWARDS || this == OUTWARDS) {
                if (shape instanceof Point) {
                    MathHelper.applyEuler(MathHelper.getRandomEuler(instance.getLevel().random), speed.set(1, 0, 0));
                } else {
                    speed.set(position);
                    if (speed.lengthSquared() != 0.0F) {
                        speed.normalize();
                    }
                    if (this == INWARDS) speed.negate();
                }
            } else {
                speed.set(direct.calculate(instance));
                if (speed.lengthSquared() != 0.0F) {
                    speed.normalize();
                }
            }
        }

        @Override
        public String toString() {
            return "Direction{" +
                    "name='" + name + '\'' +
                    ", direct=" + direct +
                    '}';
        }
    }
}

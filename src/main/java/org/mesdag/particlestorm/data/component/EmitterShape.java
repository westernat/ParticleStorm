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
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.FloatMolangExp3;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
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

    @Override
    public void update(ParticleEmitterEntity entity) {
        if (entity.spawned) return;
        if (entity.spawnDuration <= 1 || entity.age % entity.spawnDuration == 0) {
            for (int num = 0; num < entity.spawnRate; num++) {
                if (hasSpaceInParticleLimit(entity)) {
                    Vector3f position = new Vector3f();
                    Vector3f speed = new Vector3f();
                    initializeParticle(entity, position, speed);
                    if (entity.getDetail().localPosition) {
                        Vec3 emitterPos = entity.position();
                        position.add((float) emitterPos.x, (float) emitterPos.y, (float) emitterPos.z);
                    }
                    if (entity.getDetail().localRotation) {
                        applyEuler(entity.getXRot(), entity.getYRot(), 0.0F, position);
                    }
                    if (entity.getDetail().localVelocity) {
                        Vec3 emitterVec = entity.getDeltaMovement();
                        speed.add((float) emitterVec.x, (float) emitterVec.y, (float) emitterVec.z);
                    }
                    emittingParticle(entity, position, speed);
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

    protected abstract void initializeParticle(ParticleEmitterEntity entity, Vector3f position, Vector3f speed);

    private static void emittingParticle(ParticleEmitterEntity entity, Vector3f position, Vector3f speed) {
        speed.mul(entity.invTickRate);
        Particle particle = ((ParticleEngineAccessor) Minecraft.getInstance().particleEngine).callMakeParticle(entity.getDetail().option, position.x, position.y, position.z, speed.x, speed.y, speed.z);
        if (particle instanceof MolangParticleInstance instance) {
            instance.emitter = entity;
            instance.getVariableTable().subTable = entity.getVariableTable();
            instance.particleGroup = entity.particleGroup;
            instance.detail.assignments.forEach(assignment -> {
                // 重定向，防止污染变量表
                String name = assignment.variable().name();
                instance.getVariableTable().setValue(name, new Variable(name, assignment.value()));
            });
            instance.components = instance.detail.effect.components.values().stream().filter(c -> {
                if (c instanceof IParticleComponent p) {
                    if (c instanceof ParticleMotionDynamic) {
                        instance.motionDynamic = true;
                    }
                    p.apply(instance);
                    return p.requireUpdate();
                }
                return false;
            }).map(c -> (IParticleComponent) c).toList();
            if (!instance.motionDynamic) instance.setParticleSpeed(0.0, 0.0, 0.0);
        }
        Minecraft.getInstance().particleEngine.add(particle);
    }

    private static boolean hasSpaceInParticleLimit(ParticleEmitterEntity entity) {
        ParticleGroup particleGroup = entity.particleGroup;
        return ((ParticleEngineAccessor) Minecraft.getInstance().particleEngine).trackedParticleCounts().getInt(particleGroup) < particleGroup.getLimit();
    }

    protected float randomab(RandomSource random, float a, float b) {
        return a + (b - a) * random.nextFloat();
    }

    private static Vector3f getRandomEuler(RandomSource random) {
        float x = random.nextFloat() * (random.nextBoolean() ? Mth.PI : -Mth.PI);
        float y = random.nextFloat() * (random.nextBoolean() ? Mth.PI : -Mth.PI);
        float z = random.nextFloat() * (random.nextBoolean() ? Mth.PI : -Mth.PI);
        return new Vector3f(x, y, z);
    }

    private static void applyEuler(Vector3f euler, Vector3f dest) {
        new Quaternionf().rotateXYZ(euler.x, euler.y, euler.z).transform(dest);
    }

    private static void applyEuler(float x, float y, float z, Vector3f dest) {
        new Quaternionf().rotateXYZ(x, y, z).transform(dest);
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
        protected void initializeParticle(ParticleEmitterEntity entity, Vector3f position, Vector3f speed) {
            position.set(offset.calculate(entity));
            float radius = this.radius.calculate(entity);
            float op = entity.level().random.nextFloat() * Mth.TWO_PI;
            float sp = surfaceOnly ? radius : radius * Mth.sqrt(entity.level().random.nextFloat());
            position.x += sp * Mth.cos(op);
            position.z += sp * Mth.sin(op);
            float[] lp = planeNormal.plane.calculate(entity);
            if (!Arrays.equals(lp, PlaneNormal.FN)) {
                Quaternionf quaternion = planeNormal.setFromUnitVectors(PlaneNormal.VY, new Vector3f(lp), new Quaternionf());
                planeNormal.applyQuaternion(quaternion, position);
            }
            direction.apply(entity, this, position, speed);
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
            public static final float[] FX = new float[]{1.0F, 0.0F, 0.0F};
            public static final float[] FY = new float[]{0.0F, 1.0F, 0.0F};
            public static final float[] FZ = new float[]{0.0F, 0.0F, 1.0F};
            public static final Vector3f VN = new Vector3f(0.0F, 0.0F, 0.0F);
            public static final Vector3f VX = new Vector3f(1.0F, 0.0F, 0.0F);
            public static final Vector3f VY = new Vector3f(0.0F, 1.0F, 0.0F);
            public static final Vector3f VZ = new Vector3f(0.0F, 0.0F, 1.0F);
            public static final Codec<PlaneNormal> CODEC = Codec.either(Codec.STRING, FloatMolangExp3.CODEC).xmap(
                    either -> either.map(d -> switch (d) {
                        case "x" -> X;
                        case "z" -> Z;
                        default -> Y;
                    }, list -> new PlaneNormal("custom", list)),
                    plane -> Either.right(plane.plane)
            );
            public static final double EPSILON = 2.220446049250313e-16;
            public final String name;
            public final FloatMolangExp3 plane;

            PlaneNormal(String name, FloatMolangExp3 list) {
                this.name = name;
                this.plane = list;
            }

            public boolean isCustom() {
                return "custom".equals(name);
            }

            private Quaternionf setFromUnitVectors(Vector3f e, Vector3f t, Quaternionf dest) {
                float n = e.dot(t) + 1.0F;
                if (n < EPSILON) {
                    if (Math.abs(e.x) > Math.abs(e.z)) {
                        dest.x = -e.y;
                        dest.y = e.x;
                        dest.z = 0.0F;
                    } else {
                        dest.x = 0.0F;
                        dest.y = -e.z;
                        dest.z = e.y;
                    }
                    dest.w = 0.0F;
                } else {
                    dest.x = e.y * t.z - e.z * t.y;
                    dest.y = e.z * t.x - e.x * t.z;
                    dest.z = e.x * t.y - e.y * t.x;
                    dest.w = n;
                }
                return dest.normalize();
            }

            private void applyQuaternion(Quaternionf e, Vector3f dest) {
                float t = dest.x, n = dest.y, r = dest.z;
                float i = e.x, a = e.y, o = e.z, s = e.w;
                float l = s * t + a * r - o * n;
                float c = s * n + o * t - i * r;
                float u = s * r + i * n - a * t;
                float h = -i * t - a * n - o * r;
                dest.x = l * s + h * -i + c * -o - u * -a;
                dest.y = c * s + h * -a + u * -i - l * -o;
                dest.z = u * s + h * -o + l * -a - c * -i;
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
        protected void initializeParticle(ParticleEmitterEntity entity, Vector3f position, Vector3f speed) {
            position.set(offset.calculate(entity));
            float[] n = halfDimensions.calculate(entity);
            RandomSource random = entity.level().random;
            position.x = randomab(random, -n[0], n[0]);
            position.y = randomab(random, -n[1], n[1]);
            position.z = randomab(random, -n[2], n[2]);
            if (surfaceOnly) {
                int r = random.nextInt(0, 3);
                boolean i = random.nextBoolean();
                position.setComponent(r, n[r] * (i ? 1 : -1));
            }
            direction.apply(entity, this, position, speed);
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
        protected void initializeParticle(ParticleEmitterEntity entity, Vector3f position, Vector3f speed) {
            EntityDimensions dimensions = entity.attached.getDimensions(entity.attached.getPose());
            Vector3f n = new Vector3f(dimensions.width(), dimensions.height(), dimensions.width());
            RandomSource random = entity.level().random;
            position.x = randomab(random, -n.x, n.x);
            position.y = randomab(random, -n.y, n.y);
            position.z = randomab(random, -n.z, n.z);
            if (surfaceOnly) {
                int r = random.nextInt(0, 3);
                boolean i = random.nextBoolean();
                position.setComponent(r, n.get(r) * (i ? 1 : -1));
            }
            direction.apply(entity, this, position, speed);
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
        protected void initializeParticle(ParticleEmitterEntity entity, Vector3f position, Vector3f speed) {
            position.set(offset.calculate(entity));
            direction.apply(entity, this, position, speed);
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
        protected void initializeParticle(ParticleEmitterEntity entity, Vector3f position, Vector3f speed) {
            position.set(offset.calculate(entity));
            float a = radius.calculate(entity);
            position.x = surfaceOnly ? a : a * entity.level().random.nextFloat();
            applyEuler(getRandomEuler(entity.level().random), position);
            direction.apply(entity, this, position, speed);
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

        public void apply(ParticleEmitterEntity entity, EmitterShape shape, Vector3f position, Vector3f speed) {
            // todo inherited_particle_speed
            float invTickRate = entity.invTickRate;
            if (this == INWARDS || this == OUTWARDS) {
                if (shape instanceof Point) {
                    applyEuler(getRandomEuler(entity.level().random), speed.set(invTickRate, 0, 0));
                } else {
                    speed.set(position).normalize();
                    if (this == INWARDS) speed.negate();
                }
            } else { // custom
                speed.set(direct.calculate(entity)).normalize();
            }
            speed.x *= invTickRate;
            speed.y *= invTickRate;
            speed.z *= invTickRate;
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

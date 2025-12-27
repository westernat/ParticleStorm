package org.mesdag.particlestorm.data.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.mesdag.particlestorm.api.*;
import org.mesdag.particlestorm.data.MathHelper;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.FloatMolangExp3;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.EmitterPreset;
import org.mesdag.particlestorm.particle.ParticleEmitter;
import org.mesdag.particlestorm.particle.ParticlePreset;

import java.util.Arrays;
import java.util.List;

public abstract sealed class EmitterShape implements IEmitterComponent permits EmitterShape.Disc, EmitterShape.Box, EmitterShape.EntityAABB, EmitterShape.Point, EmitterShape.Sphere {
    protected final boolean surfaceOnly;

    protected EmitterShape(boolean surfaceOnly) {
        this.surfaceOnly = surfaceOnly;
    }

    /// Emit only from the edge of the shape
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
            if (entity.getPreset().emitterRateType == EmitterRate.Type.INSTANT) {
                entity.spawned = true;
            }
        }
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    protected abstract void initializeParticle(MolangInstance instance, Vector3f position, Vector3f speed);

    protected boolean isPoint() {
        return false;
    }

    private <T extends Particle & IMolangParticleInstance> void emittingParticle(ParticleEmitter emitter) {
        T instance = RegisterCustomParticleTypeEvent.createParticle(emitter);
        instance.setEmitter(emitter);

        ParticlePreset particlePreset = instance.getPreset();
        MathHelper.redirect(particlePreset.assignments, instance.getVars());

        Vector3f position = new Vector3f();
        Vector3f speed = new Vector3f();
        initializeParticle(instance, position, speed);
        for (IParticleComponent component : particlePreset.effect.orderedParticleEarlyComponents) {
            component.apply(instance);
        }
        speed.mul(instance.getInitialSpeed());
        if (emitter.parentMode == ParticleEmitter.ParentMode.LOCATOR) {
            position.x *= -1;
            position.y *= -1;
            speed.x *= -1;
            speed.y *= -1;
        }
        EmitterPreset emitterPreset = emitter.getPreset();
        if (emitter.parentMode != ParticleEmitter.ParentMode.WORLD && emitterPreset.localPosition && !emitterPreset.localRotation) {
            speed.x *= -1;
            speed.z *= -1;
        }
        if (emitterPreset.localRotation) {
            MathHelper.applyEuler(emitter.rot.x, emitter.rot.y, 0.0F, position);
        }
        if (emitterPreset.localPosition) {
            Vec3 emitterPos = emitter.getPosition();
            position.add((float) emitterPos.x, (float) emitterPos.y, (float) emitterPos.z);
        }
        speed.mul(emitter.invTickRate);
        if (emitter.getAttachedEntity() != null && emitterPreset.localVelocity) {
            Vec3 emitterVec = emitter.getAttachedEntity().getDeltaMovement();
            speed.add((float) emitterVec.x, (float) emitterVec.y, (float) emitterVec.z);
        }

        instance.setParticleSpeed(speed.x, speed.y, speed.z);
        instance.setPos(position.x, position.y, position.z);
        instance.setPosO(position.x, position.y, position.z);
        instance.setParticleGroup(emitter.particleGroup);

        for (IParticleComponent component : particlePreset.effect.orderedParticleComponents) {
            component.apply(instance);
        }
        instance.setComponents(particlePreset.effect.orderedParticleComponentsWhichRequireUpdate);
        if (!particlePreset.motionDynamic) instance.setParticleSpeed(0.0, 0.0, 0.0);
        Minecraft.getInstance().particleEngine.add(instance);
    }

    private static boolean hasSpaceInParticleLimit(ParticleEmitter emitter) {
        ParticleGroup particleGroup = emitter.particleGroup;
        return Minecraft.getInstance().particleEngine.trackedParticleCounts.getInt(particleGroup) < particleGroup.getLimit();
    }

    /// This component spawns particles using a disc shape, particles can be spawned inside the shape or on its outer perimeter.
    public static final class Disc extends EmitterShape {
        /// Specifies the offset from the emitter to emit the particles
        ///
        /// Evaluated once per particle emitted
        public final FloatMolangExp3 offset;
        /// Disc radius
        ///
        /// Evaluated once per particle emitted
        public final FloatMolangExp radius;
        /// Specifies the normal of the disc plane, the disc will be perpendicular to this direction
        public final PlaneNormal planeNormal;
        /// Specifies the direction of particles.
        public final Direction direction;

        public Disc(FloatMolangExp3 offset, FloatMolangExp radius, PlaneNormal planeNormal, Direction direction, boolean surfaceOnly) {
            super(surfaceOnly);
            this.offset = offset;
            this.radius = radius;
            this.planeNormal = planeNormal;
            this.direction = direction;
        }

        @Override
        public Deserializer<Disc> deserializer() {
            return Disc::fromJson;
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
            if (!Arrays.equals(lp, PlaneNormal.YN)) {
                MathHelper.applyQuaternion(MathHelper.setFromUnitVectors(MathHelper.Y_AXIS, new Vector3f(lp), new Quaternionf()), position);
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

        public static Disc fromJson(JsonElement element) {
            JsonObject object = element.getAsJsonObject();
            FloatMolangExp3 exp3 = FloatMolangExp3.fromJson(object.get("offset"));
            FloatMolangExp exp = FloatMolangExp.fromJson(object.get("radius"), FloatMolangExp.ONE);
            JsonElement planeNormal1 = object.get("plane_normal");
            PlaneNormal planeNormal2 = planeNormal1 == null ? PlaneNormal.Y : PlaneNormal.fromJson(planeNormal1);
            JsonElement direction1 = object.get("direction");
            Direction direction2 = direction1 == null ? Direction.OUTWARDS : Direction.fromJson(direction1);
            boolean surfaceOnly1 = GsonHelper.getAsBoolean(object, "surface_only", false);
            return new Disc(exp3, exp, planeNormal2, direction2, surfaceOnly1);
        }

        /// Custom direction for the normal
        public record PlaneNormal(String name, FloatMolangExp3 plane) {
            public static final PlaneNormal X = new PlaneNormal("x", FloatMolangExp3.X);
            public static final PlaneNormal Y = new PlaneNormal("y", FloatMolangExp3.Y);
            public static final PlaneNormal Z = new PlaneNormal("z", FloatMolangExp3.Z);
            public static final float[] YN = new float[]{0.0F, 1.0F, 0.0F};

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

            public static PlaneNormal fromJson(JsonElement element) {
                if (element.isJsonPrimitive()) {
                    String s = element.getAsString();
                    if ("x".equals(s)) {
                        return X;
                    }
                    if ("y".equals(s)) {
                        return Y;
                    }
                    if ("z".equals(s)) {
                        return Z;
                    }
                    throw new JsonParseException("Unknown Plane Normal: " + element);
                }
                if (element.isJsonArray()) {
                    return new PlaneNormal("custom", FloatMolangExp3.fromJson(element));
                }
                throw new JsonParseException("Not a(n) string or array: " + element);
            }
        }
    }

    /// All particles come out of a box of the specified size from the emitter.
    public static final class Box extends EmitterShape {
        /// Specifies the offset from the emitter to emit the particles
        /// Evaluated once per particle emitted
        public final FloatMolangExp3 offset;
        public final FloatMolangExp3 halfDimensions;
        /// Specifies the direction of particles.
        public final Direction direction;

        public Box(FloatMolangExp3 offset, FloatMolangExp3 halfDimensions, Direction direction, boolean surfaceOnly) {
            super(surfaceOnly);
            this.offset = offset;
            this.halfDimensions = halfDimensions;
            this.direction = direction;
        }

        @Override
        public Deserializer<Box> deserializer() {
            return Box::fromJson;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(
                    offset.exp1(), offset.exp2(), offset.exp3(),
                    halfDimensions.exp1(), halfDimensions.exp2(), halfDimensions.exp3(),
                    direction.direct.exp1(), direction.direct.exp2(), direction.direct.exp3()
            );
        }

        @Override
        protected void initializeParticle(MolangInstance instance, Vector3f position, Vector3f speed) {
            float[] offset = this.offset.calculate(instance);
            position.set(offset);
            float[] n = halfDimensions.calculate(instance);
            RandomSource random = instance.getLevel().random;
            position.x += Mth.nextFloat(random, -n[0], n[0]);
            position.y += Mth.nextFloat(random, -n[1], n[1]);
            position.z += Mth.nextFloat(random, -n[2], n[2]);
            if (surfaceOnly) {
                int i = random.nextInt(0, 3);
                boolean b = random.nextBoolean();
                position.setComponent(i, offset[i] + (b ? n[i] : -n[i]));
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

        public static Box fromJson(JsonElement element) {
            JsonObject object = element.getAsJsonObject();
            FloatMolangExp3 exp3 = FloatMolangExp3.fromJson(object.get("offset"));
            FloatMolangExp3 exp4 = FloatMolangExp3.fromJson(object.get("half_dimensions"));
            JsonElement direction1 = object.get("direction");
            Direction direction2 = direction1 == null ? Direction.OUTWARDS : Direction.fromJson(direction1);
            boolean surfaceOnly1 = GsonHelper.getAsBoolean(object, "surface_only", false);
            return new Box(exp3, exp4, direction2, surfaceOnly1);
        }
    }

    /// All particles come out of the axis-aligned bounding box AABB for the entity the emitter is attached to, or the emitter point if no entity.
    public static final class EntityAABB extends EmitterShape {
        public final Direction direction;

        public EntityAABB(Direction direction, boolean surfaceOnly) {
            super(surfaceOnly);
            this.direction = direction;
        }

        @Override
        public Deserializer<EntityAABB> deserializer() {
            return EntityAABB::fromJson;
        }

        @Override
        public List<MolangExp> getAllMolangExp() {
            return List.of(direction.direct.exp1(), direction.direct.exp2(), direction.direct.exp3());
        }

        @Override
        protected void initializeParticle(MolangInstance instance, Vector3f position, Vector3f speed) {
            EntityDimensions dimensions = instance.getAttachedEntity().getDimensions(instance.getAttachedEntity().getPose());
            Vector3f n = new Vector3f(dimensions.width, dimensions.height, dimensions.width).mul(0.5F);
            RandomSource random = instance.getLevel().random;
            position.x = Mth.nextFloat(random, -n.x, n.x);
            position.y = Mth.nextFloat(random, -n.y, n.y);
            position.z = Mth.nextFloat(random, -n.z, n.z);
            if (surfaceOnly) {
                int r = random.nextInt(0, 3);
                boolean i = random.nextBoolean();
                position.setComponent(r, n.get(r) * (i ? 1 : -1));
            }
            direction.apply(instance, this, position.add(0.0F, 1.0F, 0.0F), speed);
        }

        @Override
        public String toString() {
            return "EntityAABB{" +
                    "direction=" + direction +
                    ", surfaceOnly=" + surfaceOnly +
                    '}';
        }

        public static EntityAABB fromJson(JsonElement element) {
            JsonObject object = element.getAsJsonObject();
            JsonElement direction1 = object.get("direction");
            Direction direction2 = direction1 == null ? Direction.OUTWARDS : Direction.fromJson(direction1);
            boolean surfaceOnly1 = GsonHelper.getAsBoolean(object, "surface_only", false);
            return new EntityAABB(direction2, surfaceOnly1);
        }
    }

    /// All particles come out of a point offset from the emitter.
    public static final class Point extends EmitterShape {
        /// Specifies the offset from the emitter to emit the particles
        ///
        /// Evaluated once per particle emitted
        public final FloatMolangExp3 offset;
        /// Specifies the direction of particles.
        public final Direction direction;

        public Point(FloatMolangExp3 offset, Direction direction) {
            super(false);
            this.offset = offset;
            this.direction = direction;
        }

        @Override
        public Deserializer<Point> deserializer() {
            return Point::fromJson;
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

        @Override
        protected boolean isPoint() {
            return true;
        }

        public static Point fromJson(JsonElement element) {
            JsonObject object = element.getAsJsonObject();
            FloatMolangExp3 exp3 = FloatMolangExp3.fromJson(object.get("offset"));
            JsonElement direction1 = object.get("direction");
            Direction direction2 = direction1 == null ? Direction.OUTWARDS : Direction.fromJson(direction1);
            return new Point(exp3, direction2);
        }
    }

    public static final class Sphere extends EmitterShape {
        /// Specifies the offset from the emitter to emit the particles
        ///
        /// Evaluated once per particle emitted
        public final FloatMolangExp3 offset;
        /// Sphere radius
        ///
        /// Evaluated once per particle emitted
        public final FloatMolangExp radius;
        /// Specifies the direction of particles.
        public final Direction direction;

        public Sphere(FloatMolangExp3 offset, FloatMolangExp radius, Direction direction, boolean surfaceOnly) {
            super(surfaceOnly);
            this.offset = offset;
            this.radius = radius;
            this.direction = direction;
        }

        @Override
        public Deserializer<Sphere> deserializer() {
            return Sphere::fromJson;
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

        public static Sphere fromJson(JsonElement element) {
            JsonObject object = element.getAsJsonObject();
            FloatMolangExp3 exp3 = FloatMolangExp3.fromJson(object.get("offset"));
            FloatMolangExp exp = FloatMolangExp.fromJson(object.get("radius"), FloatMolangExp.ONE);
            JsonElement direction1 = object.get("direction");
            Direction direction2 = direction1 == null ? Direction.OUTWARDS : Direction.fromJson(direction1);
            boolean surfaceOnly1 = GsonHelper.getAsBoolean(object, "surface_only", false);
            return new Sphere(exp3, exp, direction2, surfaceOnly1);
        }
    }

    /// Evaluated once per particle emitted
    public record Direction(String name, FloatMolangExp3 direct) {
        /// Particle direction towards center of shape
        public static final Direction INWARDS = new Direction("inwards", FloatMolangExp3.ZERO);
        /// Particle direction away from center of shape
        public static final Direction OUTWARDS = new Direction("outwards", FloatMolangExp3.ZERO);

        public void apply(MolangInstance instance, EmitterShape shape, Vector3f position, Vector3f speed) {
            if (this == OUTWARDS && instance.getEmitter().inheritedParticleSpeed != null) {
                speed.set(instance.getEmitter().inheritedParticleSpeed);
            } else if (this == INWARDS || this == OUTWARDS) {
                if (shape.isPoint()) {
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
                if (speed.x != 0.0F || speed.y != 0.0F || speed.z != 0.0F) {
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

        public static Direction fromJson(JsonElement element) {
            if (element.isJsonPrimitive()) {
                String s = element.getAsString();
                if ("inwards".equals(s)) {
                    return INWARDS;
                }
                if ("outwards".equals(s)) {
                    return OUTWARDS;
                }
                throw new JsonParseException("Unknown direction: " + s);
            }
            if (element.isJsonArray()) {
                return new Direction("custom", FloatMolangExp3.fromJson(element));
            }
            throw new JsonParseException("Not a(n) string or array: " + element);
        }
    }
}

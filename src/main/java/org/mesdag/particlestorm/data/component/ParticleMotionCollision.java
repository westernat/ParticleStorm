package org.mesdag.particlestorm.data.component;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.molang.BoolMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * This component enables collisions between the terrain and the particle.<p>
 * Collision detection in Minecraft consists of detecting an intersection,<p>
 * moving to a nearby non-intersecting point for the particle (if possible),<p>
 * and setting its direction to not be aimed towards the collision (usually perpendicular to the collision surface).
 *
 * @param enabled                  Enables collision when true/non-zero.<p>
 *                                 Evaluated every frame
 * @param collisionDrag            Alters the speed of the particle when it has collided<p>
 *                                 Useful for emulating friction/drag when colliding, e.g a particle that hits the ground would slow to a stop.<p>
 *                                 This drag slows down the particle by this amount in blocks/sec when in contact
 * @param coefficientOfRestitution Used for bouncing/not-bouncing<p>
 *                                 Set to 0.0 to not bounce, 1.0 to bounce back up to original height and in-between to lose speed after bouncing.<p>
 *                                 Set to >1.0 to gain energy on each bounce
 * @param collisionRadius          Used to minimize interpenetration of particles with the environment<p>
 *                                 Note that this must be less than or equal to 1/2 block
 * @param expireOnContact          Triggers expiration on contact if true
 * @param events                   Triggers an event array of individual events
 */
public record ParticleMotionCollision(BoolMolangExp enabled, float collisionDrag, float coefficientOfRestitution, float collisionRadius, boolean expireOnContact, List<Event> events) implements IParticleComponent {
    public static final ResourceLocation ID = ResourceLocation.withDefaultNamespace("particle_motion_collision");
    public static final Codec<ParticleMotionCollision> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BoolMolangExp.CODEC.lenientOptionalFieldOf("enabled", BoolMolangExp.TRUE).forGetter(ParticleMotionCollision::enabled),
            Codec.FLOAT.lenientOptionalFieldOf("collision_drag", 0.0F).forGetter(ParticleMotionCollision::collisionDrag),
            Codec.FLOAT.lenientOptionalFieldOf("coefficient_of_restitution", 0.0F).forGetter(ParticleMotionCollision::coefficientOfRestitution),
            Codec.FLOAT.lenientOptionalFieldOf("collision_radius", 0.0F).forGetter(ParticleMotionCollision::collisionRadius),
            Codec.BOOL.lenientOptionalFieldOf("expire_on_contact", false).forGetter(ParticleMotionCollision::expireOnContact),
            Codec.either(Event.CODEC, Codec.list(Event.CODEC)).xmap(
                    either -> either.map(Collections::singletonList, Function.identity()),
                    l -> l.size() == 1 ? Either.left(l.getFirst()) : Either.right(l)
            ).fieldOf("events").orElseGet(List::of).forGetter(ParticleMotionCollision::events)
    ).apply(instance, ParticleMotionCollision::new));

    @Override
    public Codec<ParticleMotionCollision> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(enabled);
    }

    @Override
    public void update(MolangParticleInstance instance) {
        instance.setCollision(enabled.get(instance));
    }

    @Override
    public void apply(MolangParticleInstance instance) {
        instance.collisionDrag = collisionDrag * instance.getInvTickRate();
        instance.coefficientOfRestitution = coefficientOfRestitution;
        float radius = Math.max(collisionRadius, Mth.EPSILON);
        instance.setBoundingBox(instance.getBoundingBox().inflate(radius, 0.0, radius));
        instance.setLocationFromBoundingbox();
        instance.expireOnContact = expireOnContact;
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public String toString() {
        return "ParticleMotionCollision{" +
                "enabled=" + enabled +
                ", collisionDrag=" + collisionDrag +
                ", coefficientOfRestitution=" + coefficientOfRestitution +
                ", collisionRadius=" + collisionRadius +
                ", expireOnContact=" + expireOnContact +
                ", events=" + events +
                '}';
    }

    /**
     * @param event    Triggers the specified event if the conditions are met
     * @param minSpeed Optional minimum speed for event triggering
     */
    public record Event(String event, float minSpeed) {
        public static final Codec<Event> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("event").forGetter(Event::event),
                ExtraCodecs.POSITIVE_FLOAT.fieldOf("min_speed").orElse(2.0F).forGetter(Event::minSpeed)
        ).apply(instance, Event::new));

        @Override
        public String toString() {
            return "Event{" +
                    "event='" + event + '\'' +
                    ", minSpeed=" + minSpeed +
                    '}';
        }
    }
}

package org.mesdag.particlestorm.data.component;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.Deserializer;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.molang.BoolMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.List;

/// This component enables collisions between the terrain and the particle.
///
/// Collision detection in Minecraft consists of detecting an intersection,
///
/// moving to a nearby non-intersecting point for the particle (if possible),
///
/// and setting its direction to not be aimed towards the collision (usually perpendicular to the collision surface).
///
/// @param enabled                  Enables collision when true/non-zero.<p>
///                                 Evaluated every frame
/// @param collisionDrag            Alters the speed of the particle when it has collided<p>
///                                 Useful for emulating friction/drag when colliding, e.g a particle that hits the ground would slow to a stop.<p>
///                                 This drag slows down the particle by this amount in blocks/sec when in contact
/// @param coefficientOfRestitution Used for bouncing/not-bouncing<p>
///                                 Set to 0.0 to not bounce, 1.0 to bounce back up to original height and in-between to lose speed after bouncing.<p>
///                                 Set to >1.0 to gain energy on each bounce
/// @param collisionRadius          Used to minimize interpenetration of particles with the environment<p>
///                                 Note that this must be less than or equal to 1/2 block
/// @param expireOnContact          Triggers expiration on contact if true
/// @param events                   Triggers an event array of individual events
public record ParticleMotionCollision(
        BoolMolangExp enabled,
        float collisionDrag,
        float coefficientOfRestitution,
        float collisionRadius,
        boolean expireOnContact,
        List<Event> events
) implements IParticleComponent {
    public static final ResourceLocation ID = new ResourceLocation("particle_motion_collision");

    @Override
    public Deserializer<ParticleMotionCollision> deserializer() {
        return ParticleMotionCollision::fromJson;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(enabled);
    }

    @Override
    public void update(IMolangParticleInstance instance) {
        instance.setCollision(enabled.get(instance));
    }

    @Override
    public void apply(IMolangParticleInstance instance) {
        update(instance);
        instance.setCollisionDrag(collisionDrag * instance.getInvTickRate());
        instance.setCoefficientOfRestitution(coefficientOfRestitution);
        float radius = Math.max(collisionRadius, Mth.EPSILON);
        instance.self().setBoundingBox(instance.self().getBoundingBox().inflate(radius, 0.0, radius));
        instance.setExpireOnContact(expireOnContact);
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

    public static ParticleMotionCollision fromJson(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        JsonElement enabled1 = object.get("enabled");
        BoolMolangExp exp = enabled1 == null ? BoolMolangExp.TRUE : BoolMolangExp.fromJson(enabled1);
        float collisionDrag1 = GsonHelper.getAsFloat(object, "collision_drag", 0);
        float coefficient_of_restitution = GsonHelper.getAsFloat(object, "coefficient_of_restitution", 0);
        float collision_radius = GsonHelper.getAsFloat(object, "collision_radius", 0);
        boolean expireOnContact1 = GsonHelper.getAsBoolean(object, "expire_on_contact", false);
        ImmutableList.Builder<Event> builder = ImmutableList.builder();
        JsonElement events1 = object.get("events");
        if (events1 != null) {
            if (events1.isJsonArray()) {
                for (JsonElement jsonElement : events1.getAsJsonArray()) {
                    builder.add(Event.fromJson(jsonElement));
                }
            } else if (events1.isJsonObject()) {
                builder.add(Event.fromJson(events1));
            }
            throw new JsonParseException("Not a(n) array or object");
        }
        return new ParticleMotionCollision(exp, collisionDrag1, coefficient_of_restitution, collision_radius, expireOnContact1, builder.build());
    }

    /// @param event    Triggers the specified event if the conditions are met
    /// @param minSpeed Optional minimum speed for event triggering
    public record Event(String event, float minSpeed) {
        @Override
        public String toString() {
            return "Event{" +
                    "event='" + event + '\'' +
                    ", minSpeed=" + minSpeed +
                    '}';
        }

        public static Event fromJson(JsonElement element) {
            JsonObject object = element.getAsJsonObject();
            String s = GsonHelper.getAsString(object, "event");
            float f = ParticleStorm.positive(GsonHelper.getAsFloat(object, "min_speed", 2));
            return new Event(s, f);
        }
    }
}

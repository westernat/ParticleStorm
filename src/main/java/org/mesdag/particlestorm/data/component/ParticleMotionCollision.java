package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.mesdag.particlestorm.data.molang.BoolMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.List;

// todo events

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
 */
public record ParticleMotionCollision(BoolMolangExp enabled, float collisionDrag, float coefficientOfRestitution, float collisionRadius, boolean expireOnContact) implements IParticleComponent {
    public static final Codec<ParticleMotionCollision> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BoolMolangExp.CODEC.fieldOf("enabled").orElse(BoolMolangExp.TRUE).forGetter(ParticleMotionCollision::enabled),
            Codec.FLOAT.fieldOf("collision_drag").orElse(0.0F).forGetter(ParticleMotionCollision::collisionDrag),
            Codec.FLOAT.fieldOf("coefficient_of_restitution").orElse(0.0F).forGetter(ParticleMotionCollision::coefficientOfRestitution),
            Codec.FLOAT.fieldOf("collision_radius").orElse(0.0F).forGetter(ParticleMotionCollision::collisionRadius),
            Codec.BOOL.fieldOf("expire_on_contact").orElse(false).forGetter(ParticleMotionCollision::expireOnContact)
    ).apply(instance, ParticleMotionCollision::new));

    @Override
    public Codec<ParticleMotionCollision> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(enabled);
    }
}
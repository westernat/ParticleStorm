package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.List;

/**
 * This component specifies the frame of reference of the emitter.<p>
 * Applies only when the emitter is attached to an entity.<p>
 * When 'position' is true, the particles will simulate in entity space, otherwise they will simulate in world space.<p>
 * Rotation works the same way for rotation.<p>
 * Default is false for both, which makes the particles emit relative to the emitter, then simulate independently of the emitter.<p>
 * Note that rotation = true and position = false is an invalid option.<p>
 * Velocity will add the emitter's velocity to the initial particle velocity.
 */
public record EmitterLocalSpace(boolean position, boolean rotation, boolean velocity) implements IEmitterComponent {
    public static final Codec<EmitterLocalSpace> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("position").orElse(false).forGetter(EmitterLocalSpace::position),
            Codec.BOOL.fieldOf("rotation").orElse(false).forGetter(EmitterLocalSpace::rotation),
            Codec.BOOL.fieldOf("velocity").orElse(false).forGetter(EmitterLocalSpace::velocity)
    ).apply(instance, EmitterLocalSpace::new));

    public EmitterLocalSpace(boolean position, boolean rotation, boolean velocity) {
        this.position = position;
        this.rotation = rotation;
        this.velocity = velocity;

        if (rotation && !position) throw new IllegalArgumentException("rotation = true and position = false is an invalid option");
    }

    @Override
    public Codec<? extends IComponent> codec() {
        return null;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of();
    }

    @Override
    public String toString() {
        return "EmitterLocalSpace[" +
                "position=" + position + ", " +
                "rotation=" + rotation + ", " +
                "velocity=" + velocity + ']';
    }
}

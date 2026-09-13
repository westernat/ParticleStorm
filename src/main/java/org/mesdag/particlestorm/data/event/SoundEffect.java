package org.mesdag.particlestorm.data.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.MolangInstance;

public record SoundEffect(Holder<SoundEvent> soundEffect) implements IEventNode {
    public static final Codec<Holder<SoundEvent>> SOUND_EFFECT_CODEC = RegistryFileCodec.create(Registries.SOUND_EVENT, RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("event_name").forGetter(SoundEvent::location)
    ).apply(instance, SoundEvent::createVariableRangeEvent)));
    public static final MapCodec<SoundEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SOUND_EFFECT_CODEC.fieldOf("sound_effect").orElseGet(() -> Holder.direct(SoundEvents.EMPTY)).forGetter(SoundEffect::soundEffect)
    ).apply(instance, SoundEffect::new));

    private static final Vector3f VECTOR = new Vector3f();

    @Override
    public void execute(MolangInstance instance) {
        if (instance instanceof IMolangParticleInstance particle) {
            particle.getEmitter().local2World(VECTOR.set((float) particle.getX(), (float) particle.getY(), (float) particle.getZ()), 1.0F);
        } else {
            Vec3 position = instance.getPosition();
            VECTOR.set(position.x, position.y, position.z);
        }
        instance.getLevel().playLocalSound(VECTOR.x, VECTOR.y, VECTOR.z, soundEffect.value(), SoundSource.AMBIENT, 1.0F, 1.0F, true);
    }
}

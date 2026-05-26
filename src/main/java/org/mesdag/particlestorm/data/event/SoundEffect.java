package org.mesdag.particlestorm.data.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceLocation;
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
            ResourceLocation.CODEC.fieldOf("event_name").forGetter(SoundEvent::getLocation)
    ).apply(instance, SoundEvent::createVariableRangeEvent)));
    public static final MapCodec<SoundEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SOUND_EFFECT_CODEC.fieldOf("sound_effect").orElseGet(() -> Holder.direct(SoundEvents.EMPTY)).forGetter(SoundEffect::soundEffect)
    ).apply(instance, SoundEffect::new));

    private static final Vector3f vector3f = new Vector3f();

    @Override
    public void execute(MolangInstance instance) {
        if (instance instanceof IMolangParticleInstance p) {
            p.getEmitter().local2World(vector3f.set((float) p.getX(), (float) p.getY(), (float) p.getZ()), 1);
        } else {
            Vec3 pos = instance.getPosition();
            vector3f.set(pos.x, pos.y, pos.z);
        }
        instance.getLevel().playLocalSound(vector3f.x, vector3f.y, vector3f.z, soundEffect.value(), SoundSource.AMBIENT, 1.0F, 1.0F, true);
    }
}

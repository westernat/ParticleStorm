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
import org.mesdag.particlestorm.data.molang.MolangInstance;

public record SoundEffect(Holder<SoundEvent> soundEffect) implements IEventNode {
    public static final Codec<Holder<SoundEvent>> SOUND_EFFECT_CODEC = RegistryFileCodec.create(Registries.SOUND_EVENT, RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("event_name").forGetter(SoundEvent::getLocation)
    ).apply(instance, SoundEvent::createVariableRangeEvent)));
    public static final MapCodec<SoundEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SOUND_EFFECT_CODEC.fieldOf("sound_effect").orElseGet(() -> Holder.direct(SoundEvents.EMPTY)).forGetter(SoundEffect::soundEffect)
    ).apply(instance, SoundEffect::new));

    @Override
    public void execute(MolangInstance instance) {
        Vec3 position = instance.getPosition();
        instance.getLevel().playLocalSound(position.x, position.y, position.z, soundEffect.value(), SoundSource.AMBIENT, 1.0F, 1.0F, true);
    }
}

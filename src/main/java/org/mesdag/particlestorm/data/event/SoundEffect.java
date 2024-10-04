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

public record SoundEffect(Holder<SoundEvent> soundEffect) implements IEventNode {
    public static final Codec<Holder<SoundEvent>> SOUND_EFFECT_CODEC = RegistryFileCodec.create(Registries.SOUND_EVENT, RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("event_name").forGetter(SoundEvent::getLocation)
    ).apply(instance, SoundEvent::createVariableRangeEvent)));
    public static final MapCodec<SoundEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SOUND_EFFECT_CODEC.fieldOf("sound_effect").orElseGet(() -> Holder.direct(SoundEvents.EMPTY)).forGetter(SoundEffect::soundEffect)
    ).apply(instance, SoundEffect::new));

    @Override
    public MapCodec<SoundEffect> codec() {
        return CODEC;
    }

    @Override
    public String name() {
        return "sound_effect";
    }
}

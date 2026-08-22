package org.mesdag.particlestorm.api;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.event.IModBusEvent;
import org.mesdag.particlestorm.data.event.ParticleEffect;
import org.mesdag.particlestorm.particle.ParticleEmitter;
import org.mesdag.particlestorm.particle.attach.WithBlockParticleEmitter;

import java.util.Map;
import java.util.function.BiFunction;

public class RegisterCustomEmitterTypeEvent extends Event implements IModBusEvent {
    private static final Map<ResourceLocation, BiFunction<Level, CompoundTag, ? extends ParticleEmitter>> fromNbt = new Object2ObjectOpenHashMap<>();
    private static final Map<ResourceLocation, BiFunction<ParticleEmitter, ParticleEffect, ? extends ParticleEmitter>> fromEffect = new Object2ObjectOpenHashMap<>();

    private RegisterCustomEmitterTypeEvent() {}

    public static void postEvent() {
        if (fromNbt.isEmpty()) {
            RegisterCustomEmitterTypeEvent event = new RegisterCustomEmitterTypeEvent();
            event.register(WithBlockParticleEmitter.TYPE, WithBlockParticleEmitter::new, WithBlockParticleEmitter::new);
//            event.register(PresetVarsParticleEmitter.TYPE, PresetVarsParticleEmitter::new, PresetVarsParticleEmitter::new);
            ModLoader.get().postEvent(event);
        }
    }

    public <E extends ParticleEmitter> void register(ResourceLocation type, BiFunction<Level, CompoundTag, E> nbt, BiFunction<ParticleEmitter, ParticleEffect, E> effect) {
        if (fromNbt.put(type, nbt) != null) {
            throw new IllegalStateException("Duplicated emitter type: " + type);
        }
        fromEffect.put(type, effect);
    }

    public static ParticleEmitter create(Level level, CompoundTag tag) {
        return fromNbt.getOrDefault(ResourceLocation.tryParse(tag.getString(ParticleEmitter.TYPE_KEY)), ParticleEmitter::new).apply(level, tag);
    }

    public static ParticleEmitter create(ParticleEmitter parent, ParticleEffect effect) {
        return fromEffect.getOrDefault(parent.type, ParticleEmitter::new).apply(parent, effect);
    }
}

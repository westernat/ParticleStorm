package org.mesdag.particlestorm.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.event.IModBusEvent;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

public class RegisterCustomEmitterTypeEvent extends Event implements IModBusEvent {
    public static final String TYPE_KEY = "type";
    private static Map<ResourceLocation, BiFunction<Level, CompoundTag, ? extends ParticleEmitter>> map;

    private RegisterCustomEmitterTypeEvent() {}

    public static void postEvent() {
        map = new HashMap<>();
        ModLoader.get().postEvent(new RegisterCustomEmitterTypeEvent());
    }

    public <E extends ParticleEmitter> void register(ResourceLocation id, BiFunction<Level, CompoundTag, E> factory) {
        map.put(id, factory);
    }

    public static ParticleEmitter create(Level level, CompoundTag tag) {
        if (tag.contains(TYPE_KEY, Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(Objects.requireNonNull(tag.get(TYPE_KEY)).getAsString());
            BiFunction<Level, CompoundTag, ? extends ParticleEmitter> factory = map.get(id);
            if (factory != null) {
                return factory.apply(level, tag);
            }
        }
        return new ParticleEmitter(level, tag);
    }
}

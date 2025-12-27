package org.mesdag.particlestorm.data.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.api.MolangInstance;

public record SoundEffect(Holder<SoundEvent> soundEffect) implements IEventNode {
    @Override
    public void execute(MolangInstance instance) {
        Vec3 position = instance.getPosition();
        instance.getLevel().playLocalSound(position.x, position.y, position.z, soundEffect.value(), SoundSource.AMBIENT, 1.0F, 1.0F, true);
    }

    public static SoundEffect fromJson(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        JsonElement soundEffect1 = object.get("sound_effect");
        Holder<SoundEvent> holder;
        if (soundEffect1 != null) {
            ResourceLocation eventName = new ResourceLocation(GsonHelper.getAsString(soundEffect1.getAsJsonObject(), "event_name"));
            holder = ForgeRegistries.SOUND_EVENTS.getDelegateOrThrow(eventName);
        } else {
            holder = Holder.direct(SoundEvents.EMPTY);
        }
        return new SoundEffect(holder);
    }
}

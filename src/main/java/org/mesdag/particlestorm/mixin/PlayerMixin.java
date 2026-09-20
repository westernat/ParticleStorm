package org.mesdag.particlestorm.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.mesdag.particlestorm.mixed.IPlayerPersistentData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin implements IPlayerPersistentData {
    @Unique
    private CompoundTag particlestorm$persistentData = new CompoundTag();

    @Override
    public CompoundTag particlestorm$getPersistentData() {
        return particlestorm$persistentData;
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void particlestorm$readPersistentData(CompoundTag input, CallbackInfo ci) {
        this.particlestorm$persistentData = input.contains(TAG_KEY, 10)
                ? input.getCompound(TAG_KEY)
                : new CompoundTag();
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void particlestorm$writePersistentData(CompoundTag output, CallbackInfo ci) {
        if (!particlestorm$persistentData.isEmpty()) {
            output.put(TAG_KEY, particlestorm$persistentData);
        }
    }
}

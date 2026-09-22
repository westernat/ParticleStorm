package org.mesdag.particlestorm.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
    private void particlestorm$readPersistentData(ValueInput input, CallbackInfo ci) {
        this.particlestorm$persistentData = input.read(TAG_KEY, CompoundTag.CODEC).orElseGet(CompoundTag::new);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void particlestorm$writePersistentData(ValueOutput output, CallbackInfo ci) {
        if (!particlestorm$persistentData.isEmpty()) {
            output.store(TAG_KEY, CompoundTag.CODEC, particlestorm$persistentData);
        }
    }
}

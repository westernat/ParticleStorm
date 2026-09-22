package org.mesdag.particlestorm.mixed;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public interface IPlayerPersistentData {
    String TAG_KEY = "ParticleStormPersistentData";

    CompoundTag particlestorm$getPersistentData();

    static IPlayerPersistentData of(Player player) {
        return (IPlayerPersistentData) player;
    }
}

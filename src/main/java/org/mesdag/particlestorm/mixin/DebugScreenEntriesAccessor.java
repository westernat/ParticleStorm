package org.mesdag.particlestorm.mixin;

import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DebugScreenEntries.class)
public interface DebugScreenEntriesAccessor {
    /// The vanilla register(String, DebugScreenEntry) is private; expose it so ParticleStorm can add F3 entries.
    @Invoker("register")
    static Identifier particlestorm$invokeRegister(String name, DebugScreenEntry entry) {
        throw new AssertionError();
    }
}

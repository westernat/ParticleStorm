package org.mesdag.particlestorm;

import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;
import org.mesdag.particlestorm.particle.MolangParticleEngine;

/// F3 debug entries (26.2 DebugScreenEntries API): live Molang particle and ParticleStorm emitter counts.
public final class PSDebugEntries {
    public static final Identifier MOLANG = ParticleStorm.asResource("molang_particles");

    private PSDebugEntries() {
    }

    public static void register(RegisterDebugEntriesEvent event) {
        event.register(MOLANG, new MolangEntry());
        event.includeInProfile(MOLANG, DebugScreenProfile.DEFAULT, DebugScreenEntryStatus.ALWAYS_ON);
    }

    private static final class MolangEntry implements DebugScreenEntry {
        @Override
        public void display(DebugScreenDisplayer displayer, Level level, LevelChunk clientChunk, LevelChunk serverChunk) {
            displayer.addLine("MolangParticle: " + MolangParticleEngine.INSTANCE.totalParticleCount() + ". " +
                    "ParticleEmitter: " + MolangParticleEngine.INSTANCE.totalEmitterCount());
        }

        @Override
        public boolean isAllowed(boolean showDebugCharts) {
            return true;
        }
    }
}

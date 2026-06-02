package org.mesdag.particlestorm.api;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

import java.util.concurrent.Executor;

public abstract class MolangParticleLoadEvent extends Event implements IModBusEvent {
    private final Executor executor;

    public MolangParticleLoadEvent(Executor executor) {
        this.executor = executor;
    }

    public Executor getExecutor() {
        return executor;
    }

    /// In Background Executor {@link net.minecraft.Util#backgroundExecutor()}
    public static class Pre extends MolangParticleLoadEvent {
        public Pre(Executor executor) {
            super(executor);
        }
    }

    /// In Game Executor {@link net.minecraft.client.Minecraft}
    public static class Post extends MolangParticleLoadEvent {
        public Post(Executor executor) {
            super(executor);
        }
    }
}

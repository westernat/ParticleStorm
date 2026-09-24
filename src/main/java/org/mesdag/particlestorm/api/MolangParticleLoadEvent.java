package org.mesdag.particlestorm.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.concurrent.Executor;

public abstract class MolangParticleLoadEvent {
    private final Executor executor;

    public MolangParticleLoadEvent(Executor executor) {
        this.executor = executor;
    }

    public Executor getExecutor() {
        return executor;
    }

    public static class Pre extends MolangParticleLoadEvent {
        public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class, listeners -> event -> {
            for (Callback listener : listeners) {
                listener.onLoad(event);
            }
        });

        @FunctionalInterface
        public interface Callback {
            void onLoad(Pre event);
        }

        public Pre(Executor executor) {
            super(executor);
        }
    }

    /// In Game Executor {@link net.minecraft.client.Minecraft}
    public static class Post extends MolangParticleLoadEvent {
        public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class, listeners -> event -> {
            for (Callback listener : listeners) {
                listener.onLoad(event);
            }
        });

        @FunctionalInterface
        public interface Callback {
            void onLoad(Post event);
        }

        public Post(Executor executor) {
            super(executor);
        }
    }
}

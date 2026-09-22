package org.mesdag.particlestorm.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import org.mesdag.particlestorm.data.molang.VariableTable;

public class AddDefaultVariableEvent {
    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class, listeners -> event -> {
        for (Callback listener : listeners) {
            listener.onAddDefaultVariables(event);
        }
    });

    @FunctionalInterface
    public interface Callback {
        void onAddDefaultVariables(AddDefaultVariableEvent event);
    }

    private final VariableTable table;

    public AddDefaultVariableEvent(VariableTable vt) {
        this.table = vt;
    }

    public VariableTable getTable() {
        return table;
    }

    public static class Entity extends AddDefaultVariableEvent {
        private final net.minecraft.world.entity.Entity entity;

        public Entity(VariableTable vt, net.minecraft.world.entity.Entity entity) {
            super(vt);
            this.entity = entity;
        }

        public net.minecraft.world.entity.Entity getEntity() {
            return entity;
        }
    }

    public static class BlockEntity extends AddDefaultVariableEvent {
        private final net.minecraft.world.level.block.entity.BlockEntity entity;

        public BlockEntity(VariableTable vt, net.minecraft.world.level.block.entity.BlockEntity entity) {
            super(vt);
            this.entity = entity;
        }

        public net.minecraft.world.level.block.entity.BlockEntity getEntity() {
            return entity;
        }
    }
}

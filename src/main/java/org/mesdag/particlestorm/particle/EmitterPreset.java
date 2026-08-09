package org.mesdag.particlestorm.particle;

import net.minecraft.core.particles.ParticleType;
import net.neoforged.fml.ModLoader;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.EmitterPresetLoadedEvent;
import org.mesdag.particlestorm.api.IEmitterComponent;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.data.component.*;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.data.molang.compiler.MolangParser;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class EmitterPreset {
    public ParticleType<?> type;
    public List<IEmitterComponent> components;
    public Map<String, Map<String, IEventNode>> events;
    public VariableTable vars;
    public EmitterRate.Type emitterRateType = EmitterRate.Type.MANUAL;
    public boolean localPosition = false;
    public boolean localRotation = false;
    public boolean localVelocity = false;
    public EmitterLifetimeEvents lifetimeEvents;

    /// For custom preset data
    protected Map<Class<?>, Object> tickets;

    public EmitterPreset(ParticleType<?> type, List<IEmitterComponent> components, Map<String, Map<String, IEventNode>> events) {
        this.type = type;
        this.components = components;
        this.events = events;
        VariableTable table = new VariableTable(addDefaultVariables(), null);
        MolangParser parser = new MolangParser(table);
        boolean lifeTime = false;
        boolean rate = false;
        boolean shape = false;
        for (IEmitterComponent component : components) {
            if (component instanceof EmitterLifetime) {
                if (lifeTime) {
                    throw new IllegalArgumentException("Duplicate emitter lifetime component");
                }
                lifeTime = true;
            } else if (component instanceof EmitterRate) {
                if (rate) {
                    throw new IllegalArgumentException("Duplicate emitter rate component");
                }
                rate = true;
                switch (component) {
                    case EmitterRate.Instant ignored -> this.emitterRateType = EmitterRate.Type.INSTANT;
                    case EmitterRate.Steady ignored -> this.emitterRateType = EmitterRate.Type.STEADY;
                    default -> this.emitterRateType = EmitterRate.Type.MANUAL;
                }
            } else if (component instanceof EmitterShape) {
                if (shape) {
                    throw new IllegalArgumentException("Duplicate emitter shape component");
                }
                shape = true;
            } else if (component instanceof EmitterLocalSpace(boolean position, boolean rotation, boolean velocity)) {
                this.localPosition = position;
                this.localRotation = rotation;
                this.localVelocity = velocity;
            } else if (component instanceof EmitterLifetimeEvents e) {
                this.lifetimeEvents = e;
            }
            for (MolangExp exp : component.getAllMolangExp()) {
                exp.compile(parser);
            }
        }

        this.vars = table;
        ModLoader.postEvent(new EmitterPresetLoadedEvent(this));
    }

    public <T> void setTicket(Class<T> clazz, T value) {
        if (tickets == null) this.tickets = new HashMap<>();
        tickets.put(clazz, value);
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable T getTicket(Class<T> clazz) {
        return tickets == null ? null : (T) tickets.get(clazz);
    }

    private static Hashtable<String, Variable> addDefaultVariables() {
        Hashtable<String, Variable> table = new Hashtable<>();
        table.computeIfAbsent("variable.emitter_age", s -> new Variable(s, i -> i.getEmitter().tickAge()));
        table.computeIfAbsent("variable.emitter_lifetime", s -> new Variable(s, i -> i.getEmitter().tickLifetime()));
        table.computeIfAbsent("variable.emitter_random_1", s -> new Variable(s, i -> i.getEmitter().emitterRandom1));
        table.computeIfAbsent("variable.emitter_random_2", s -> new Variable(s, i -> i.getEmitter().emitterRandom2));
        table.computeIfAbsent("variable.emitter_random_3", s -> new Variable(s, i -> i.getEmitter().emitterRandom3));
        table.computeIfAbsent("variable.emitter_random_4", s -> new Variable(s, i -> i.getEmitter().emitterRandom4));
        return table;
    }
}

package org.mesdag.particlestorm.particle;

import org.jetbrains.annotations.NotNull;
import org.mesdag.particlestorm.data.component.*;
import org.mesdag.particlestorm.data.event.IEventNode;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.data.molang.compiler.MathParser;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.value.CompoundValue;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.data.molang.compiler.value.VariableAssignment;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class EmitterDetail {
    public final MolangParticleOption option;
    public final List<IEmitterComponent> components;
    public final Map<String, Map<String, IEventNode>> events;
    public final VariableTable variableTable;
    public final ArrayList<VariableAssignment> assignments;
    public EmitterRate.Type emitterRateType = EmitterRate.Type.MANUAL;
    public boolean localPosition = false;
    public boolean localRotation = false;
    public boolean localVelocity = false;
    public EmitterLifetimeEvents lifetimeEvents;

    public EmitterDetail(MolangParticleOption option, List<IEmitterComponent> components, Map<String, Map<String, IEventNode>> events) {
        this.option = option;
        this.components = components;
        this.events = events;
        VariableTable table = new VariableTable(addDefaultVariables(), null);
        MathParser parser = new MathParser(table);
        ArrayList<VariableAssignment> toInit = new ArrayList<>();
        boolean lifeTime = false;
        boolean rate = false;
        boolean shape = false;
        for (IEmitterComponent component : components) {
            if (component instanceof EmitterLifetime) {
                if (lifeTime) throw new IllegalArgumentException("Duplicate emitter lifetime component");
                else lifeTime = true;
            } else if (component instanceof EmitterRate) {
                if (rate) {
                    throw new IllegalArgumentException("Duplicate emitter rate component");
                } else {
                    rate = true;
                    switch (component) {
                        case EmitterRate.Instant ignored -> this.emitterRateType = EmitterRate.Type.INSTANT;
                        case EmitterRate.Steady ignored -> this.emitterRateType = EmitterRate.Type.STEADY;
                        default -> this.emitterRateType = EmitterRate.Type.MANUAL;
                    }
                }
            } else if (component instanceof EmitterShape) {
                if (shape) throw new IllegalArgumentException("Duplicate emitter shape component");
                else shape = true;
            } else if (component instanceof EmitterLocalSpace localSpace) {
                this.localPosition = localSpace.position();
                this.localRotation = localSpace.rotation();
                this.localVelocity = localSpace.velocity();
            } else if (component instanceof EmitterLifetimeEvents e) {
                this.lifetimeEvents = e;
            }
            component.getAllMolangExp().forEach(exp -> {
                exp.compile(parser);
                MathValue variable = exp.getVariable();
                if (variable != null && !forAssignment(table.table, toInit, variable)) {
                    forCompound(table.table, toInit, variable);
                }
            });
        }

        this.variableTable = table;
        this.assignments = toInit;
    }

    private static @NotNull Hashtable<String, Variable> addDefaultVariables() {
        Hashtable<String, Variable> table = new Hashtable<>();
        table.computeIfAbsent("variable.emitter_age", s -> new Variable(s, i -> i.getEmitter().tickAge()));
        table.computeIfAbsent("variable.emitter_lifetime", s -> new Variable(s, i -> i.getEmitter().tickLifetime()));
        table.computeIfAbsent("variable.emitter_random_1", s -> new Variable(s, i -> i.getEmitter().emitterRandom1));
        table.computeIfAbsent("variable.emitter_random_2", s -> new Variable(s, i -> i.getEmitter().emitterRandom2));
        table.computeIfAbsent("variable.emitter_random_3", s -> new Variable(s, i -> i.getEmitter().emitterRandom3));
        table.computeIfAbsent("variable.emitter_random_4", s -> new Variable(s, i -> i.getEmitter().emitterRandom4));
        return table;
    }

    private static boolean forAssignment(Hashtable<String, Variable> table, ArrayList<VariableAssignment> toInit, MathValue value) {
        if (value instanceof VariableAssignment assignment) {
            Variable variable = assignment.variable();
            table.put(variable.name(), variable);
            toInit.add(assignment);
            return true;
        }
        return false;
    }

    private static void forCompound(Hashtable<String, Variable> table, ArrayList<VariableAssignment> toInit, MathValue variable) {
        if (variable instanceof CompoundValue compoundValue) {
            for (MathValue value : compoundValue.subValues()) {
                forAssignment(table, toInit, value);
            }
        }
    }
}

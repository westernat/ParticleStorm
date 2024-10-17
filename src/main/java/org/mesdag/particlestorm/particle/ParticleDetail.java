package org.mesdag.particlestorm.particle;

import net.minecraft.client.particle.ParticleRenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.data.component.*;
import org.mesdag.particlestorm.data.molang.MolangInstance;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.MolangParser;
import org.mesdag.particlestorm.data.molang.compiler.value.CompoundValue;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.data.molang.compiler.value.VariableAssignment;

import java.util.ArrayList;
import java.util.Hashtable;

import static org.mesdag.particlestorm.data.molang.compiler.MolangQueries.applyPrefixAliases;

@OnlyIn(Dist.CLIENT)
public class ParticleDetail {
    public final DefinedParticleEffect effect;
    public final ParticleRenderType renderType;
    public final FaceCameraMode facingCameraMode;
    public final float minSpeedThresholdSqr;
    public final boolean environmentLighting;
    public ParticleLifeTimeEvents lifeTimeEvents;

    public final VariableTable variableTable;
    public final ArrayList<VariableAssignment> assignments;

    public ParticleDetail(DefinedParticleEffect effect) {
        this.effect = effect;
        this.renderType = switch (effect.description.parameters().material()) {
            case TERRAIN_SHEET -> ParticleRenderType.TERRAIN_SHEET;
            case PARTICLE_SHEET_OPAQUE -> ParticleRenderType.PARTICLE_SHEET_OPAQUE;
            case PARTICLE_SHEET_TRANSLUCENT -> ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
            case PARTICLE_SHEET_LIT -> ParticleRenderType.PARTICLE_SHEET_LIT;
            case CUSTOM -> ParticleRenderType.CUSTOM;
            case NO_RENDER -> ParticleRenderType.NO_RENDER;
        };
        IComponent component1 = effect.components.get(ParticleAppearanceBillboard.ID);
        if (component1 == null) throw new NullPointerException("No particle_appearance_billboard here");
        ParticleAppearanceBillboard particleAppearanceBillboard = (ParticleAppearanceBillboard) component1;
        this.facingCameraMode = FaceCameraMode.valueOf(particleAppearanceBillboard.faceCameraMode().name());
        this.minSpeedThresholdSqr = particleAppearanceBillboard.direction().minSpeedThreshold() * particleAppearanceBillboard.direction().minSpeedThreshold();
        this.environmentLighting = effect.components.containsValue(ParticleAppearanceLighting.INSTANCE);
        this.lifeTimeEvents = (ParticleLifeTimeEvents) effect.components.get(ParticleLifeTimeEvents.ID);

        VariableTable table = new VariableTable(addDefaultVariables(), null);
        MolangParser parser = new MolangParser(table);
        effect.curves.forEach((key, value) -> {
            value.input.compile(parser);
            value.horizontalRange.compile(parser);
            value.nodes.either.ifRight(exps -> exps.forEach(exp -> exp.compile(parser)));
            String name = applyPrefixAliases(key, "variable.", "v.");
            table.table.put(name, new Variable(name, p -> value.calculate(p, name)));
        });

        ArrayList<VariableAssignment> toInit = new ArrayList<>();
        for (IComponent component : effect.components.values()) {
            if (!(component instanceof IParticleComponent)) continue;
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
        table.computeIfAbsent("variable.particle_age", s -> new Variable(s, MolangInstance::tickAge));
        table.computeIfAbsent("variable.particle_lifetime", s -> new Variable(s, MolangInstance::tickLifetime));
        table.computeIfAbsent("variable.particle_random_1", s -> new Variable(s, MolangInstance::getRandom1));
        table.computeIfAbsent("variable.particle_random_2", s -> new Variable(s, MolangInstance::getRandom2));
        table.computeIfAbsent("variable.particle_random_3", s -> new Variable(s, MolangInstance::getRandom3));
        table.computeIfAbsent("variable.particle_random_4", s -> new Variable(s, MolangInstance::getRandom4));
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

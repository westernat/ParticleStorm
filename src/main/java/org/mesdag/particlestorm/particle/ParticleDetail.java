package org.mesdag.particlestorm.particle;

import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.mesdag.particlestorm.data.ParticleEffect;
import org.mesdag.particlestorm.data.component.IComponent;
import org.mesdag.particlestorm.data.component.ParticleAppearanceBillboard;
import org.mesdag.particlestorm.data.component.ParticleAppearanceLighting;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.data.molang.compiler.MathParser;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.value.CompoundValue;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.data.molang.compiler.value.VariableAssignment;

import java.util.ArrayList;
import java.util.Hashtable;

import static org.mesdag.particlestorm.data.molang.compiler.MolangQueries.applyPrefixAliases;

@OnlyIn(Dist.CLIENT)
public class ParticleDetail {
    public final ParticleEffect effect;
    public final ResourceLocation id;
    public final ParticleRenderType renderType;
    public final SingleQuadParticle.FacingCameraMode facingCameraMode;
    public final float minSpeedThresholdSqr;
    public final boolean environmentLighting;

    public final VariableTable variableTable;
    public final ArrayList<VariableAssignment> assignments;

    public ParticleDetail(ParticleEffect effect) {
        this.effect = effect;
        this.id = effect.description.identifier();
        this.renderType = switch (effect.description.parameters().material()) {
            case TERRAIN_SHEET -> ParticleRenderType.TERRAIN_SHEET;
            case PARTICLE_SHEET_OPAQUE -> ParticleRenderType.PARTICLE_SHEET_OPAQUE;
            case PARTICLE_SHEET_TRANSLUCENT -> ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
            case PARTICLE_SHEET_LIT -> ParticleRenderType.PARTICLE_SHEET_LIT;
            case CUSTOM -> ParticleRenderType.CUSTOM;
            case NO_RENDER -> ParticleRenderType.NO_RENDER;
        };
        ParticleAppearanceBillboard particleAppearanceBillboard = (ParticleAppearanceBillboard) effect.components.get(ParticleAppearanceBillboard.ID);
        this.facingCameraMode = switch (particleAppearanceBillboard.faceCameraMode()) {
            case ROTATE_XYZ -> FaceCameraMode.ROTATE_XYZ;
            case ROTATE_Y -> FaceCameraMode.ROTATE_Y;
            case LOOKAT_XYZ -> SingleQuadParticle.FacingCameraMode.LOOKAT_XYZ;
            case LOOKAT_Y -> SingleQuadParticle.FacingCameraMode.LOOKAT_Y;
            case DIRECTION_X -> FaceCameraMode.DIRECTION_X;
            case DIRECTION_Y -> FaceCameraMode.DIRECTION_Y;
            case DIRECTION_Z -> FaceCameraMode.DIRECTION_Z;
            case EMITTER_TRANSFORM_XY -> FaceCameraMode.EMITTER_TRANSFORM_XY;
            case EMITTER_TRANSFORM_XZ -> FaceCameraMode.EMITTER_TRANSFORM_XZ;
            case EMITTER_TRANSFORM_YZ -> FaceCameraMode.EMITTER_TRANSFORM_YZ;
        };
        this.minSpeedThresholdSqr = particleAppearanceBillboard.direction().minSpeedThreshold() * particleAppearanceBillboard.direction().minSpeedThreshold();
        this.environmentLighting = effect.components.get(ParticleAppearanceLighting.ID) != null;

        Hashtable<String, Variable> table = addDefaultVariables();
        MathParser parser = new MathParser(table);
        effect.curves.keySet().forEach(s -> {
            String name = applyPrefixAliases(s, "variable.", "v.");
            table.put(name, new Variable(name, parser.compileMolang(name)));
        });

        ArrayList<VariableAssignment> toInit = new ArrayList<>();
        for (IComponent component : effect.components.values()) {
            component.getAllMolangExp().forEach(exp -> {
                exp.compile(parser);
                MathValue variable = exp.getVariable();
                if (variable != null && !forAssignment(table, toInit, variable)) {
                    forCompound(table, toInit, variable);
                }
            });
        }
        this.variableTable = new VariableTable(table, null);
        this.assignments = toInit;
    }

    private static @NotNull Hashtable<String, Variable> addDefaultVariables() {
        Hashtable<String, Variable> table = new Hashtable<>();
        table.computeIfAbsent("variable.particle_age", s -> new Variable(s, MolangParticleInstance::getAge));
        table.computeIfAbsent("variable.particle_lifetime", s -> new Variable(s, MolangParticleInstance::getLifetime));
        table.computeIfAbsent("variable.particle_random_1", s -> new Variable(s, p -> p.particleRandom1));
        table.computeIfAbsent("variable.particle_random_2", s -> new Variable(s, p -> p.particleRandom2));
        table.computeIfAbsent("variable.particle_random_3", s -> new Variable(s, p -> p.particleRandom3));
        table.computeIfAbsent("variable.particle_random_4", s -> new Variable(s, p -> p.particleRandom4));
        return table;
    }

    private static boolean forAssignment(Hashtable<String, Variable> table, ArrayList<VariableAssignment> toInit, MathValue value) {
        if (value instanceof VariableAssignment assignment) {
            Variable variable = assignment.variable();
            // 重定向，防止污染变量表
            variable.set(p -> p.variableTable.table.computeIfAbsent(variable.name(), s -> new Variable(s, assignment.value())).get(p));
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

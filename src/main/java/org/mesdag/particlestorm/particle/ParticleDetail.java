package org.mesdag.particlestorm.particle;

import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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
    public final ParticleRenderType renderType;
    public final SingleQuadParticle.FacingCameraMode facingCameraMode;
    public final float minSpeedThresholdSqr;
    public final boolean environmentLighting;

    public final VariableTable variableTable;
    public final ArrayList<VariableAssignment> toInit;

    public ParticleDetail(ParticleEffect effect) {
        this.effect = effect;
        this.renderType = switch (effect.getDescription().parameters().material()) {
            case TERRAIN_SHEET -> ParticleRenderType.TERRAIN_SHEET;
            case PARTICLE_SHEET_OPAQUE -> ParticleRenderType.PARTICLE_SHEET_OPAQUE;
            case PARTICLE_SHEET_TRANSLUCENT -> ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
            case PARTICLE_SHEET_LIT -> ParticleRenderType.PARTICLE_SHEET_LIT;
            case CUSTOM -> ParticleRenderType.CUSTOM;
            case NO_RENDER -> ParticleRenderType.NO_RENDER;
        };
        ParticleAppearanceBillboard particleAppearanceBillboard = (ParticleAppearanceBillboard) effect.getComponents().get(ParticleAppearanceBillboard.ID);
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
        this.environmentLighting = effect.getComponents().get(ParticleAppearanceLighting.ID) != null;

        Hashtable<String, Variable> table = new Hashtable<>();
        ArrayList<VariableAssignment> toInit = new ArrayList<>();
        table.computeIfAbsent("variable.particle_age", s -> new Variable(s, MolangParticleInstance::getAge));
        table.computeIfAbsent("variable.particle_lifetime", s -> new Variable(s, MolangParticleInstance::getLifetime));

        MathParser parser = new MathParser(table);
        effect.getCurves().keySet().forEach(s -> {
            String name = applyPrefixAliases(s, "variable.", "v.");
            table.put(name, new Variable(name, parser.compileMolang(name)));
        });
        for (IComponent component : effect.getComponents().values()) {
            component.getAllMolangExp().forEach(exp -> {
                exp.compile(parser);
                MathValue variable = exp.getVariable();
                if (variable != null && !forAssignment(table, toInit, variable)) {
                    forCompound(table, toInit, variable);
                }
            });
        }
        this.variableTable = new VariableTable(table);
        this.toInit = toInit;
    }

    private static boolean forAssignment(Hashtable<String, Variable> table, ArrayList<VariableAssignment> toInit, MathValue value) {
        if (value instanceof VariableAssignment assignment) {
            table.put(assignment.variable().name(), assignment.variable());
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

package org.mesdag.particlestorm.particle;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.mesdag.particlestorm.data.ParticleEffect;
import org.mesdag.particlestorm.data.component.IComponent;
import org.mesdag.particlestorm.data.component.ParticleAppearanceBillboard;
import org.mesdag.particlestorm.data.component.ParticleAppearanceLighting;
import org.mesdag.particlestorm.data.molang.ParticleVariable;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.data.molang.compiler.MathParser;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.value.CompoundValue;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.data.molang.compiler.value.VariableAssignment;

import static org.mesdag.particlestorm.data.molang.compiler.MolangQueries.applyPrefixAliases;

@OnlyIn(Dist.CLIENT)
public class ParticleDetail {
    private static final ImmutableList<String> BUILTIN_VARIABLES = ImmutableList.<String>builder().add(
            "variable.emitter_age",
            "variable.emitter_lifetime",
            "variable.emitter_random_1",
            "variable.emitter_random_2",
            "variable.emitter_random_3",
            "variable.emitter_random_4",
            "variable.entity_scale",
            "variable.particle_age",
            "variable.particle_lifetime",
            "variable.particle_random_1",
            "variable.particle_random_2",
            "variable.particle_random_3",
            "variable.particle_random_4"
    ).build();
    public final ParticleEffect effect;
    public final ParticleRenderType renderType;
    public final SingleQuadParticle.FacingCameraMode facingCameraMode;
    public final float minSpeedThresholdSqr;
    public final boolean environmentLighting;

    public final VariableTable variableTable;

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

        Object2ObjectAVLTreeMap<String, ParticleVariable> table = new Object2ObjectAVLTreeMap<>();
        for (String builtin : BUILTIN_VARIABLES) {
            table.put(builtin, new ParticleVariable(new Variable(builtin, 0), 0));
        }
        MathParser parser = new MathParser(table);
        effect.getCurves().keySet().forEach(s -> table.put(s, parser.compileMolang(applyPrefixAliases(s, "variable.", "v."))));
        for (IComponent component : effect.getComponents().values()) {
            component.getAllMolangExp().forEach(exp -> {
                exp.compile(parser);
                ParticleVariable variable = exp.getVariable();
                if (variable != null && forAssignment(table, variable.raw(), variable)) {
                    forCompound(table, variable);
                }
            });
        }
        this.variableTable = new VariableTable(table);
    }

    private static boolean forAssignment(Object2ObjectAVLTreeMap<String, ParticleVariable> table, MathValue value, ParticleVariable variable) {
        if (value instanceof VariableAssignment assignment) {
            table.put(assignment.variable().name(), variable);
            return true;
        }
        return false;
    }

    private static void forCompound(Object2ObjectAVLTreeMap<String, ParticleVariable> table, ParticleVariable variable) {
        if (variable.raw() instanceof CompoundValue compoundValue) {
            for (MathValue value : compoundValue.subValues()) {
                forAssignment(table, value, variable);
            }
        }
    }
}

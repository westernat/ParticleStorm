package org.mesdag.particlestorm.particle;

import net.minecraft.client.particle.ParticleRenderType;
import org.jetbrains.annotations.NotNull;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.api.IComponent;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.data.MathHelper;
import org.mesdag.particlestorm.data.component.*;
import org.mesdag.particlestorm.data.curve.ParticleCurve;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.MolangParser;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.data.molang.compiler.value.VariableAssignment;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import static org.mesdag.particlestorm.data.molang.compiler.MolangQueries.applyPrefixAliases;

public class ParticlePreset {
    public final DefinedParticleEffect effect;
    public final ParticleRenderType renderType;
    public final FaceCameraMode facingCameraMode;
    public final float minSpeedThresholdSqr;
    public final boolean environmentLighting;
    public ParticleLifeTimeEvents lifeTimeEvents;
    public List<ParticleMotionCollision.Event> collisionEvents = List.of();
    public final float invTextureWidth;
    public final float invTextureHeight;
    public boolean motionDynamic;

    public final VariableTable vars;
    public final List<VariableAssignment> assignments;

    public ParticlePreset(DefinedParticleEffect effect) {
        this.effect = effect;
        this.renderType = switch (effect.description.parameters().material()) {
            case TERRAIN_SHEET -> ParticleRenderType.TERRAIN_SHEET;
            case particles_opaque, PARTICLE_SHEET_OPAQUE -> ParticleRenderType.PARTICLE_SHEET_OPAQUE;
            case particles_add -> PSGameClient.PARTICLE_ADD;
            case particles_blend, PARTICLE_SHEET_TRANSLUCENT -> ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
            case particles_alpha, PARTICLE_SHEET_LIT -> ParticleRenderType.PARTICLE_SHEET_LIT;
            case CUSTOM -> ParticleRenderType.CUSTOM;
            default -> ParticleRenderType.NO_RENDER;
        };
        IComponent component1 = effect.components.get(ParticleAppearanceBillboard.ID);
        if (component1 == null) throw new NullPointerException("No particle_appearance_billboard here");
        ParticleAppearanceBillboard particleAppearanceBillboard = (ParticleAppearanceBillboard) component1;
        this.facingCameraMode = FaceCameraMode.valueOf(particleAppearanceBillboard.faceCameraMode().name());
        this.minSpeedThresholdSqr = particleAppearanceBillboard.direction().minSpeedThreshold() * particleAppearanceBillboard.direction().minSpeedThreshold();
        this.invTextureWidth = 1.0F / particleAppearanceBillboard.uv().texturewidth();
        this.invTextureHeight = 1.0F / particleAppearanceBillboard.uv().textureheight();
        this.environmentLighting = effect.components.containsValue(ParticleAppearanceLighting.INSTANCE);
        this.lifeTimeEvents = (ParticleLifeTimeEvents) effect.components.get(ParticleLifeTimeEvents.ID);
        ParticleMotionCollision motionCollision = (ParticleMotionCollision) effect.components.get(ParticleMotionCollision.ID);
        if (motionCollision != null) this.collisionEvents = motionCollision.events();
        this.motionDynamic = effect.components.get(ParticleMotionDynamic.ID) != null;

        VariableTable table = new VariableTable(addDefaultVariables(), null);
        MolangParser parser = new MolangParser(table);
        for (Map.Entry<String, ParticleCurve> entry : effect.curves.entrySet()) {
            ParticleCurve curve = entry.getValue();
            curve.input.compile(parser);
            curve.horizontalRange.compile(parser);
            curve.nodes.either.ifRight(exps -> {
                for (FloatMolangExp exp : exps) {
                    exp.compile(parser);
                }
            });
            String name = applyPrefixAliases(entry.getKey(), "variable.", "v.");
            table.table.put(name, new Variable(name, p -> curve.calculate(p, name)));
        }

        List<VariableAssignment> toInit = new ArrayList<>();
        for (IParticleComponent component : effect.orderedParticleComponents) {
            for (MolangExp exp : component.getAllMolangExp()) {
                exp.compile(parser);
                MathValue variable = exp.getVariable();
                if (variable != null && !MathHelper.forAssignment(table.table, toInit, variable)) {
                    MathHelper.forCompound(table.table, toInit, variable);
                }
            }
        }
        this.vars = table;
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
}

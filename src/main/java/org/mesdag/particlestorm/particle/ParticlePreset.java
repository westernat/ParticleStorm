package org.mesdag.particlestorm.particle;

import com.google.common.collect.Iterables;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.api.ParticlePresetLoadedEvent;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.data.MathHelper;
import org.mesdag.particlestorm.data.component.*;
import org.mesdag.particlestorm.data.curve.ParticleCurve;
import org.mesdag.particlestorm.data.description.DescriptionMaterial;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.MolangParser;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.data.molang.compiler.value.VariableAssignment;

import java.util.*;

import static org.mesdag.particlestorm.data.molang.compiler.MolangQueries.applyPrefixAliases;

public class ParticlePreset {
    public DefinedParticleEffect effect;
    public ParticleRenderType renderType;
    public FaceCameraMode facingCameraMode;
    public float minSpeedThresholdSqr;
    public boolean environmentLighting;
    public ParticleLifeTimeEvents lifeTimeEvents;
    public List<ParticleMotionCollision.Event> collisionEvents = List.of();
    public float invTextureWidth;
    public float invTextureHeight;
    public boolean motionDynamic;
    public @Nullable FloatMolangExp perUpdateExpression;

    public VariableTable vars;
    public List<VariableAssignment> assignments;

    /// For custom preset data
    protected Map<Class<?>, Object> tickets;

    public ParticlePreset(DefinedParticleEffect effect) {
        this.effect = effect;
        DescriptionMaterial material = effect.description.parameters().material();
        if (material == DescriptionMaterial.TERRAIN_SHEET) {
            this.renderType = ParticleRenderType.TERRAIN_SHEET;
        } else if (material == DescriptionMaterial.particles_opaque || material == DescriptionMaterial.PARTICLE_SHEET_OPAQUE) {
            this.renderType = ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        } else if (material == DescriptionMaterial.particles_add) {
            this.renderType = PSGameClient.PARTICLE_ADD;
        } else if (material == DescriptionMaterial.particles_blend) {
            this.renderType = PSGameClient.PARTICLE_BLEND;
        } else if (material == DescriptionMaterial.PARTICLE_SHEET_TRANSLUCENT) {
            this.renderType = ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        } else if (material == DescriptionMaterial.particles_alpha || material == DescriptionMaterial.PARTICLE_SHEET_LIT) {
            this.renderType = ParticleRenderType.PARTICLE_SHEET_LIT;
        } else if (material == DescriptionMaterial.CUSTOM) {
            this.renderType = ParticleRenderType.CUSTOM;
        } else {
            this.renderType = ParticleRenderType.NO_RENDER;
        }
        if (effect.components.get(ParticleAppearanceBillboard.ID) instanceof ParticleAppearanceBillboard component) {
            this.facingCameraMode = FaceCameraMode.fromComponent(component.faceCameraMode());
            this.minSpeedThresholdSqr = Mth.square(component.direction().minSpeedThreshold());
            this.invTextureWidth = 1.0F / component.uv().texturewidth();
            this.invTextureHeight = 1.0F / component.uv().textureheight();
        } else {
            this.facingCameraMode = FaceCameraMode.DO_NOTHING;
            this.minSpeedThresholdSqr = 0;
            this.invTextureWidth = 1;
            this.invTextureHeight = 1;
        }
        this.environmentLighting = effect.components.containsValue(ParticleAppearanceLighting.INSTANCE);
        this.lifeTimeEvents = (ParticleLifeTimeEvents) effect.components.get(ParticleLifeTimeEvents.ID);
        ParticleMotionCollision motionCollision = (ParticleMotionCollision) effect.components.get(ParticleMotionCollision.ID);
        if (motionCollision != null) this.collisionEvents = motionCollision.events();
        this.motionDynamic = effect.components.get(ParticleMotionDynamic.ID) != null;
        ParticleInitialization initialization = (ParticleInitialization) effect.components.get(ParticleInitialization.ID);
        if (initialization != null) this.perUpdateExpression = initialization.perUpdateExpression();

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
        for (IParticleComponent component : Iterables.concat(effect.orderedParticleEarlyComponents, effect.orderedParticleComponents)) {
            assert component != null;
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
        MinecraftForge.EVENT_BUS.post(new ParticlePresetLoadedEvent(effect, this));
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
        table.computeIfAbsent("variable.particle_age", s -> new Variable(s, MolangInstance::tickAge));
        table.computeIfAbsent("variable.particle_lifetime", s -> new Variable(s, MolangInstance::tickLifetime));
        table.computeIfAbsent("variable.particle_random_1", s -> new Variable(s, MolangInstance::getRandom1));
        table.computeIfAbsent("variable.particle_random_2", s -> new Variable(s, MolangInstance::getRandom2));
        table.computeIfAbsent("variable.particle_random_3", s -> new Variable(s, MolangInstance::getRandom3));
        table.computeIfAbsent("variable.particle_random_4", s -> new Variable(s, MolangInstance::getRandom4));
        return table;
    }
}

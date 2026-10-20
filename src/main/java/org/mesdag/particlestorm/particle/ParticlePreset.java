package org.mesdag.particlestorm.particle;

import com.google.common.collect.Iterables;
import net.minecraft.client.particle.ParticleRenderType;
import net.neoforged.fml.ModLoader;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.api.*;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.data.component.*;
import org.mesdag.particlestorm.data.curve.ParticleCurve;
import org.mesdag.particlestorm.data.description.DescriptionMaterial;
import org.mesdag.particlestorm.data.event.NodeMolangExp;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.data.molang.compiler.MolangParser;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import static org.mesdag.particlestorm.data.molang.compiler.MolangQueries.applyPrefixAliases;

public class ParticlePreset {
    public DefinedParticleEffect effect;
    public ParticleRenderType renderType;
    public FaceCameraMode facingCameraMode;
    public boolean environmentLighting;
    public ParticleLifeTimeEvents lifeTimeEvents;
    public List<ParticleMotionCollision.Event> collisionEvents = List.of();
    public float invTextureWidth;
    public float invTextureHeight;
    public boolean motionDynamic;
    public @Nullable ParticleInitialization initialization;

    public VariableTable vars;

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
            this.invTextureWidth = 1.0F / component.uv().texturewidth();
            this.invTextureHeight = 1.0F / component.uv().textureheight();
        } else {
            this.facingCameraMode = FaceCameraMode.DO_NOTHING;
            this.invTextureWidth = 1;
            this.invTextureHeight = 1;
        }
        this.environmentLighting = effect.components.containsValue(ParticleAppearanceLighting.INSTANCE);
        this.lifeTimeEvents = (ParticleLifeTimeEvents) effect.components.get(ParticleLifeTimeEvents.ID);
        ParticleMotionCollision motionCollision = (ParticleMotionCollision) effect.components.get(ParticleMotionCollision.ID);
        VariableTable table = new VariableTable(addDefaultVariables(), null);
        MolangParser parser = new MolangParser(table);
        if (motionCollision != null) {
            this.collisionEvents = motionCollision.events();
            for (ParticleMotionCollision.Event event : collisionEvents) {
                Map<String, IEventNode> nodes = effect.events.get(event.event());
                if (nodes == null) {
                    throw new IllegalStateException("Unknown event id: " + event.event());
                }
                for (IEventNode node : nodes.values()) {
                    if (node instanceof NodeMolangExp exp) {
                        exp.compile(parser);
                    }
                }
            }
        }
        this.motionDynamic = effect.components.containsKey(ParticleMotionDynamic.ID);
        this.initialization = (ParticleInitialization) effect.components.get(ParticleInitialization.ID);
        for (Map.Entry<String, ParticleCurve> entry : effect.curves.entrySet()) {
            String curveKey = entry.getKey();
            ParticleCurve curve = entry.getValue();
            curve.input.compile(parser);
            curve.horizontalRange.compile(parser);
            curve.nodes.either.ifRight(exps -> {
                for (FloatMolangExp exp : exps) {
                    exp.compile(parser);
                }
            });
            String name = applyPrefixAliases(curveKey, "variable.", "v.");
            // 变量值**不闭包捕获**这里的 `curve`：组件表达式编译一次就把这个 Variable 焊死在表达式树里，
            // 而编辑器换曲线时只重建 `DefinedParticleEffect` 外壳（`effect.components` 原样复用），
            // 捕获旧曲线会让「改曲线后引用它的组件（广告牌外观等）永远读旧值」。
            // 这里按曲线名去**实例所属预设**的 `effect#curves` 里现查：编辑器重新发射换了曲线，旧表达式也能读到新曲线；
            // 非粒子实例（发射器侧求值）拿不到粒子预设，退回构建时的那条曲线。
            table.table.put(name, new Variable(name, p -> {
                if (p instanceof IMolangParticleInstance particle) {
                    ParticleCurve latest = particle.getPreset().effect.curves.get(curveKey);
                    return latest == null ? 0.0F : latest.calculate(p, name);
                }
                return curve.calculate(p, name);
            }));
        }
        for (IParticleComponent component : Iterables.concat(effect.orderedParticleEarlyComponents, effect.orderedParticleComponents)) {
            assert component != null;
            for (MolangExp exp : component.getAllMolangExp()) {
                exp.compile(parser);
            }
        }
        this.vars = table;
        ModLoader.postEvent(new ParticlePresetLoadedEvent(this));
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

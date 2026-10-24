package org.mesdag.particlestorm.editor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.data.event.ParticleEffect;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.compiler.MolangParser;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.particle.EmitterPreset;
import org.mesdag.particlestorm.particle.ParticleEmitter;
import org.mesdag.particlestorm.particle.ParticlePreset;
import org.mesdag.particlestorm.particle.screen.ScreenParticleEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mesdag.particlestorm.data.molang.compiler.MolangQueries.applyPrefixAliases;

public class EditorParticleEmitter extends ScreenParticleEmitter {
    public static final ResourceLocation TYPE = ParticleStorm.asResource("editor");

    /// 编辑器缓存效果的中转位：`super(...)` 的参数先于父类构造求值，
    /// 因此可以在进入 `ParticleEmitter` 构造之前把效果放好，供下面覆写的 [#getEmitterPreset] 使用。
    private static @Nullable DefinedParticleEffect pendingEffect;
    /// 编辑器「发射器变量表」（`variable.<名字>` → molang 表达式），重建预设时编译成赋值表达式。
    private static @Nullable Map<String, String> pendingVariables;

    private @Nullable EmitterPreset editorPreset;
    private @Nullable ParticlePreset editorParticlePreset;
    /// 变量表编译出来的赋值表达式（`variable.x = expr;variable.y = expr`）。
    /// 注意：**不能**写成带初始化的 final 字段——[#getEmitterPreset] 在父类构造期间就会被调用，
    /// 那时子类字段初始化还没执行，会被随后的 `= new ArrayList<>()` 冲掉。
    private @Nullable List<MolangExp> variableExpressions;

    public EditorParticleEmitter(ParticleEmitter parent, ParticleEffect effect) {
        super(parent, effect);
    }

    public EditorParticleEmitter(Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression,
                                 @Nullable DefinedParticleEffect effect, @Nullable Map<String, String> variables) {
        super(stash(effect, variables), level, pos, particleId, expression);
    }

    private static ResourceLocation stash(@Nullable DefinedParticleEffect effect, @Nullable Map<String, String> variables) {
        pendingEffect = effect;
        pendingVariables = variables;
        return TYPE;
    }

    private static @Nullable DefinedParticleEffect takePendingEffect() {
        DefinedParticleEffect effect = pendingEffect;
        pendingEffect = null;
        return effect;
    }

    private static @Nullable Map<String, String> takePendingVariables() {
        Map<String, String> variables = pendingVariables;
        pendingVariables = null;
        return variables;
    }

    @Override
    public @Nullable EmitterPreset getEmitterPreset() {
        DefinedParticleEffect effect = takePendingEffect();
        Map<String, String> variables = takePendingVariables();
        if (effect != null) {
            this.editorPreset = new EmitterPreset(effect.description.type(), effect.orderedEmitterComponents, effect.events);
            this.editorParticlePreset = new ParticlePreset(effect);
            this.compileVariableExpressions(variables);
        }
        return this.editorPreset != null ? this.editorPreset : super.getEmitterPreset();
    }

    @Override
    public ParticlePreset getParticlePreset() {
        return this.editorParticlePreset != null ? this.editorParticlePreset : super.getParticlePreset();
    }

    /// 变量注入：不改 [EmitterPreset]，而是拿它已经建好的变量表建一个 [MolangParser]，
    /// 用 `new MolangExp(Map)` 把 `变量名 → 表达式` 编译成 `variable.x = 表达式;…` 这样的赋值表达式。
    ///
    /// 编译本身就会把变量名登记进 `preset.vars`（`MolangParser#getVariableFor` → `computeIfAbsent`），
    /// 赋值则在发射器实例上求值时执行（`VariableAssignment#get` 调 `instance.getVars().setValue`），
    /// 所以同名变量（包括 `v.emitter_random_1` 这类默认变量）会被覆盖。
    private void compileVariableExpressions(@Nullable Map<String, String> variables) {
        this.variableExpressions = new ArrayList<>();
        if (variables == null || variables.isEmpty() || this.editorPreset == null) {
            return;
        }
        MolangParser parser = new MolangParser(this.editorPreset.vars);
        // 先把这批变量在预设变量表里登记成常量 0：编译赋值表达式时 `getVariableFor` 只会拿到这个已存在的常量，
        // 变量之间互相引用（甚至自引用）时读到 0 而不是走「实例表 → 父表 → 自己」的无限递归求值。
        variables.keySet().forEach(rawName -> {
            String name = applyPrefixAliases(rawName, "variable.", "v.");
            this.editorPreset.vars.table.putIfAbsent(name, new Variable(name, 0.0F));
        });
        try {
            MolangExp expression = new MolangExp(variables);
            expression.compile(parser);
            this.variableExpressions.add(expression);
        } catch (RuntimeException e) {
            // 整批里有非法表达式：逐个编译，留下能用的；实在编译不了的登记成 0，
            // 否则组件里引用这个变量时会因为变量从未被赋值而无限递归求值。
            variables.forEach((name, exp) -> {
                try {
                    MolangExp expression = new MolangExp(Map.of(name, exp));
                    expression.compile(parser);
                    this.variableExpressions.add(expression);
                } catch (RuntimeException ignored) {
                    try {
                        MolangExp zero = new MolangExp(Map.of(name, "0"));
                        zero.compile(parser);
                        this.variableExpressions.add(zero);
                    } catch (RuntimeException ignored2) {
                        // 变量名本身非法：放弃这一条
                    }
                }
            });
        }
    }

    /// 先算变量赋值，再走正常 tick：组件里的 molang 可能在 update 里读这些变量。
    @Override
    public void tick() {
        this.applyVariableExpressions();
        super.tick();
    }

    @Override
    protected void createVars() {
        // super 会调用 [#getEmitterPreset]，变量赋值表达式在那一刻编译好；这里先执行一次，
        // 让组件初始化（`createComponents`）就能读到变量值。
        super.createVars();
        this.applyVariableExpressions();
    }

    private void applyVariableExpressions() {
        List<MolangExp> expressions = this.variableExpressions;
        if (expressions == null || expressions.isEmpty()) {
            return;
        }
        for (MolangExp expression : expressions) {
            expression.calculate(this);
        }
    }
}

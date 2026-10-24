package org.mesdag.particlestorm.editor;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.IEventNode;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.data.event.*;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.*;

/// 「事件」页签：编辑 `DefinedParticleEffect#events`（组名 → 节点树）。
///
/// 节点在 JSON 里的键就是**节点类型名**（`IEventNode.CODEC` 是按名字分发的 map），所以编辑器不提供“键”字段：
/// 每个节点类型在同一容器里最多出现一次（多份同类节点请放进 `sequence`）。添加规则：
/// - `sequence` / `randomize` 是容器节点，只能加进**空容器**；加进来之后容器里不能再加其它节点，只能进它们的子节点列表里加；
/// - 容器里已经有 `particle_effect` / `sound_effect` / `log` 之一时，不允许再添加 `particle_effect`、`sequence`、`randomize`
///   （即这几个“动作 + 容器”组合不再开放），其它可加类型是还没出现过的 `log` / `expression` / `sound_effect`；
/// - 已存在的类型不再出现在下拉里（同容器同名键不允许重复）。
///
/// `weight` 不是可添加节点：它只是 `randomize` 条目的权重（条目编辑器里额外一行）。
///
/// 三级视图：组列表 → 组内节点列表 → 节点编辑器；`sequence` / `randomize` 的「子节点」进入下一层同样的列表视图。
final class EventsTab extends EditorTab {
    private static final int LIST_ROW_HEIGHT = 12;

    /// 可添加的节点类型。
    private static final List<String> NODE_TYPES = List.of("log", "expression", "sound_effect", "particle_effect",
            "sequence", "randomize");
    /// 叶子节点：同一容器里每种最多一个，且出现任意一种后只能再加其它种类的叶子节点。
    private static final List<String> LEAF_TYPES = List.of("log", "expression", "sound_effect", "particle_effect");

    // region 草稿树

    private abstract static class NodeDraft {
        final String key;

        NodeDraft(String key) {
            this.key = key;
        }

        abstract String type();

        abstract IEventNode build();

        String summary() {
            return this.type();
        }

        boolean isContainer() {
            return false;
        }
    }

    private static final class LogDraft extends NodeDraft {
        String message;

        LogDraft(String message) {
            super("log");
            this.message = message;
        }

        @Override
        String type() {
            return "log";
        }

        @Override
        IEventNode build() {
            return new EventLog(this.message);
        }

        @Override
        String summary() {
            return "log: " + this.message;
        }
    }

    private static final class ExpressionDraft extends NodeDraft {
        String exp;
        boolean log;

        ExpressionDraft(String exp, boolean log) {
            super("expression");
            this.exp = exp;
            this.log = log;
        }

        @Override
        String type() {
            return "expression";
        }

        @Override
        IEventNode build() {
            return new NodeMolangExp(this.exp, this.log);
        }

        @Override
        String summary() {
            return "expression: " + this.exp;
        }
    }

    private static final class SoundDraft extends NodeDraft {
        String eventName;

        SoundDraft(String eventName) {
            super("sound_effect");
            this.eventName = eventName;
        }

        @Override
        String type() {
            return "sound_effect";
        }

        @Override
        IEventNode build() {
            ResourceLocation id = ResourceLocation.tryParse(this.eventName.trim());
            if (id == null) {
                throw new IllegalArgumentException("Invalid sound event: " + this.eventName);
            }
            return new SoundEffect(Holder.direct(SoundEvent.createVariableRangeEvent(id)));
        }

        @Override
        String summary() {
            return "sound_effect: " + this.eventName;
        }
    }

    private static final class ParticleEffectDraft extends NodeDraft {
        String effect;
        int type;
        String preEffectExpression;
        final List<SharedVarDraft> sharedVars = new ArrayList<>();

        ParticleEffectDraft(String effect, int type, String preEffectExpression, List<ParticleEffect.SharedVar> sharedVars) {
            super("particle_effect");
            this.effect = effect;
            this.type = type;
            this.preEffectExpression = preEffectExpression;
            for (ParticleEffect.SharedVar shared : sharedVars) {
                this.sharedVars.add(new SharedVarDraft(shared.name(), sharedVarText(shared.defaultValue())));
            }
        }

        @Override
        String type() {
            return "particle_effect";
        }

        @Override
        IEventNode build() {
            ResourceLocation id = ResourceLocation.tryParse(this.effect.trim());
            if (id == null) {
                throw new IllegalArgumentException("Invalid particle effect: " + this.effect);
            }
            MolangExp pre = this.preEffectExpression.isBlank() ? MolangExp.EMPTY : new MolangExp(this.preEffectExpression.trim());
            List<ParticleEffect.SharedVar> vars = new ArrayList<>(this.sharedVars.size());
            for (SharedVarDraft draft : this.sharedVars) {
                String name = draft.name.trim();
                if (!name.isEmpty()) {
                    vars.add(new ParticleEffect.SharedVar(name,
                            draft.defaultValue.isBlank() ? null : sharedVarDefault(draft.defaultValue)));
                }
            }
            return new ParticleEffect(id, ParticleEffect.Type.values()[this.type], pre, List.copyOf(vars));
        }

        @Override
        String summary() {
            return "particle_effect: " + this.effect;
        }
    }

    /// `shared_vars` 草稿：变量名 + 可选默认值（空 = 沿用父发射器里的变量）。
    private static final class SharedVarDraft {
        String name;
        String defaultValue;

        SharedVarDraft(String name, String defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
        }
    }

    private static String sharedVarText(@Nullable FloatMolangExp exp) {
        if (exp == null) {
            return "";
        }
        return exp.getExpStr().isEmpty() ? String.valueOf(exp.getConstant()) : exp.getExpStr();
    }

    private static FloatMolangExp sharedVarDefault(String text) {
        String trimmed = text.trim();
        try {
            return FloatMolangExp.ofConstant(Float.parseFloat(trimmed));
        } catch (NumberFormatException e) {
            return FloatMolangExp.ofExpression(trimmed);
        }
    }

    private static final class SequenceDraft extends NodeDraft {
        final List<NodeDraft> children = new ArrayList<>();

        SequenceDraft() {
            super("sequence");
        }

        @Override
        String type() {
            return "sequence";
        }

        @Override
        boolean isContainer() {
            return true;
        }

        @Override
        IEventNode build() {
            List<Map<String, IEventNode>> list = new ArrayList<>();
            for (NodeDraft child : this.children) {
                list.add(Map.of(child.key, child.build()));
            }
            return new EventSequence(list);
        }

        @Override
        String summary() {
            return "sequence (" + this.children.size() + ")";
        }
    }

    private static final class WeightDraft extends NodeDraft {
        int value = 1;

        WeightDraft() {
            super("weight");
        }

        @Override
        String type() {
            return "weight";
        }

        @Override
        IEventNode build() {
            return new EventRandomize.Weight(Math.max(1, this.value));
        }

        @Override
        String summary() {
            return "weight: " + this.value;
        }
    }

    private static final class RandomizeEntry {
        int weight = 1;
        NodeDraft node;
    }

    private static final class RandomizeDraft extends NodeDraft {
        final List<RandomizeEntry> entries = new ArrayList<>();

        RandomizeDraft() {
            super("randomize");
        }

        @Override
        String type() {
            return "randomize";
        }

        @Override
        boolean isContainer() {
            return true;
        }

        @Override
        IEventNode build() {
            List<Map<String, IEventNode>> list = new ArrayList<>();
            for (RandomizeEntry entry : this.entries) {
                Map<String, IEventNode> map = new LinkedHashMap<>();
                map.put("weight", new EventRandomize.Weight(Math.max(1, entry.weight)));
                map.put(entry.node.key, entry.node.build());
                list.add(map);
            }
            return new EventRandomize(list);
        }

        @Override
        String summary() {
            return "randomize (" + this.entries.size() + ")";
        }
    }

    // endregion

    /// 当前容器（组、或某个 sequence/randomize 节点的子节点列表）。
    private record Frame(String title, Either<List<NodeDraft>, List<RandomizeEntry>> either) {
        static Frame ofNodes(String title, List<NodeDraft> nodes) {
            return new Frame(title, Either.left(nodes));
        }

        static Frame ofEntries(String title, List<RandomizeEntry> entries) {
            return new Frame(title, Either.right(entries));
        }
    }

    private final Map<String, List<NodeDraft>> groups = new LinkedHashMap<>();
    private final Deque<Frame> frames = new ArrayDeque<>();
    private final List<ComponentGrid.Row> rows = new ArrayList<>();
    private final DropdownWidget addDropdown;
    private boolean rowsDirty = true;
    private boolean dirty;
    @Nullable
    private NodeDraft editing;
    @Nullable
    private RandomizeEntry editingEntry;
    @Nullable
    private DefinedParticleEffect loadedFrom;
    /// 本次下拉实际展示的类型（空容器会有占位项，这里记录真实类型列表用于取下标）。
    private List<String> addTypes = List.of();

    EventsTab(EditorScreen screen, Font font) {
        super(screen, font);
        this.addDropdown = new DropdownWidget(2, 0, Math.max(20, screen.toolboxWidth() - 4), LIST_ROW_HEIGHT);
    }

    @Override
    Component title() {
        return Component.translatable("particle_editor.tab.events");
    }

    // region 载入 / 同步

    private void ensureLoaded() {
        DefinedParticleEffect effect = this.screen.effect;
        if (effect == this.loadedFrom) {
            return;
        }
        this.loadedFrom = effect;
        this.groups.clear();
        this.frames.clear();
        this.editing = null;
        this.editingEntry = null;
        this.dirty = false;
        this.rowsDirty = true;
        if (effect == null) {
            return;
        }
        effect.events.forEach((name, nodes) -> {
            List<NodeDraft> list = new ArrayList<>();
            nodes.forEach((key, node) -> list.add(fromNode(node)));
            this.groups.put(name, list);
        });
    }

    private static NodeDraft fromNode(IEventNode node) {
        return switch (node) {
            case EventLog log -> new LogDraft(log.log());
            case NodeMolangExp exp -> new ExpressionDraft(exp.getExpStr(), exp.shouldLog());
            case SoundEffect sound -> new SoundDraft(sound.soundEffect().value().getLocation().toString());
            case ParticleEffect effect -> new ParticleEffectDraft(effect.effect().toString(), effect.type().ordinal(),
                    effect.preEffectExpression() == MolangExp.EMPTY ? "" : effect.preEffectExpression().getExpStr(),
                    effect.sharedVars());
            case EventSequence sequence -> {
                SequenceDraft draft = new SequenceDraft();
                sequence.nodes().forEach(map -> map.forEach((childKey, child) -> draft.children.add(fromNode(child))));
                yield draft;
            }
            case EventRandomize randomize -> {
                RandomizeDraft draft = new RandomizeDraft();
                for (Map<String, IEventNode> map : randomize.nodes) {
                    RandomizeEntry entry = new RandomizeEntry();
                    List<NodeDraft> others = new ArrayList<>();
                    for (Map.Entry<String, IEventNode> e : map.entrySet()) {
                        if (e.getKey().equals("weight") && e.getValue() instanceof EventRandomize.Weight(int value)) {
                            entry.weight = value;
                        } else {
                            others.add(fromNode(e.getValue()));
                        }
                    }
                    if (others.size() == 1) {
                        entry.node = others.getFirst();
                    } else {
                        SequenceDraft folded = new SequenceDraft();
                        folded.children.addAll(others);
                        entry.node = folded;
                    }
                    draft.entries.add(entry);
                }
                yield draft;
            }
            case EventRandomize.Weight weight -> {
                WeightDraft draft = new WeightDraft();
                draft.value = weight.value();
                yield draft;
            }
            default -> new LogDraft(String.valueOf(node));
        };
    }

    /// 把草稿树重建为 `events` map 并替换效果（解析失败的字段抛异常 → 显示错误、保留旧效果）。
    private void apply() {
        this.dirty = false;
        DefinedParticleEffect effect = this.screen.effect;
        if (effect == null) {
            return;
        }
        try {
            Map<String, Map<String, IEventNode>> events = new LinkedHashMap<>();
            for (Map.Entry<String, List<NodeDraft>> group : this.groups.entrySet()) {
                Map<String, IEventNode> nodes = new LinkedHashMap<>();
                for (NodeDraft node : group.getValue()) {
                    if (nodes.containsKey(node.key)) {
                        throw new IllegalArgumentException("Duplicate event node type in one event: " + node.key);
                    }
                    nodes.put(node.key, node.build());
                }
                events.put(group.getKey(), nodes);
            }
            this.screen.effect = new DefinedParticleEffect(effect.description, effect.curves, effect.components, events);
            // 自建的草稿就是这份效果的数据源：记下来，避免下一帧被 ensureLoaded 当成「换了粒子」而重置视图
            this.loadedFrom = this.screen.effect;
            this.screen.error = null;
            this.screen.manager.clearScene();
            if (this.screen.selectedParticle != null) {
                this.screen.spawnEmitter(this.screen.selectedParticle);
            }
        } catch (RuntimeException e) {
            this.screen.error = Component.literal(String.valueOf(e.getMessage()));
        }
    }

    private void markDirty() {
        this.dirty = true;
    }

    private void flushDirty() {
        if (this.dirty) {
            this.apply();
        }
    }

    // endregion

    // region 视图

    /// 内容滚动偏移（像素）：内容超出屏幕高度时用滚轮滚。
    private int scroll;
    private int contentTop;

    @Override
    void render(GuiGraphics guiGraphics, int contentTop, int mouseX, int mouseY) {
        this.ensureLoaded();
        this.contentTop = contentTop;
        if (this.rowsDirty) {
            this.rebuildRows();
        }
        int viewBottom = this.screen.height - 2;
        int contentHeight = 0;
        for (ComponentGrid.Row row : this.rows) {
            contentHeight += row.height();
        }
        int maxScroll = Math.max(0, contentHeight - Math.max(1, viewBottom - contentTop));
        this.scroll = net.minecraft.util.Mth.clamp(this.scroll, 0, maxScroll);
        guiGraphics.enableScissor(0, contentTop, this.screen.toolboxWidth(), viewBottom);
        try {
            int y = contentTop + 2 - this.scroll;
            for (ComponentGrid.Row row : this.rows) {
                row.render(guiGraphics, y, mouseX, mouseY);
                y += row.height();
            }
            for (ComponentGrid.Row row : this.rows) {
                if (row instanceof ComponentGrid.SelectRow select && select.dropdownOpen()) {
                    select.renderMenu(guiGraphics, mouseX, mouseY);
                }
            }
            this.addDropdown.render(guiGraphics, this.font, mouseX, mouseY, viewBottom);
        } finally {
            guiGraphics.disableScissor();
        }
        if (this.addDropdown.isOpen()) {
            String item = this.addDropdown.item(this.addDropdown.hitTest(mouseX, mouseY));
            if (item != null && this.font.width(item) > this.addDropdown.textWidth()) {
                this.screen.hoveredTooltip = Component.literal(item);
            }
        }
    }

    private void rebuildRows() {
        this.rowsDirty = false;
        this.rows.clear();
        if (this.frames.isEmpty()) {
            this.addGroupRows();
        } else if (this.editing != null) {
            this.addNodeEditorRows();
        } else {
            this.addContainerRows();
        }
    }

    private void addGroupRows() {
        this.rows.add(new ComponentGrid.TextRow(this.screen, this.font,
                () -> eventLabel("new"), "", text -> {
            String name = text.trim();
            if (!name.isEmpty() && !this.groups.containsKey(name)) {
                this.groups.put(name, new ArrayList<>());
                this.rowsDirty = true;
                this.markDirty();
            }
        }));
        for (String name : this.groups.keySet()) {
            List<NodeDraft> nodes = this.groups.get(name);
            this.rows.add(new ComponentGrid.ButtonRow(this.screen, this.font, name + " (" + nodes.size() + ")", () -> {
                this.frames.addLast(Frame.ofNodes(name, nodes));
                this.editing = null;
                this.rowsDirty = true;
            }));
        }
    }

    private void addContainerRows() {
        Frame frame = this.frames.getLast();
        this.rows.add(this.backRow());
        frame.either.ifRight(entries -> {
            for (RandomizeEntry entry : entries) {
                this.rows.add(new ComponentGrid.ButtonRow(this.screen, this.font,
                        "w" + entry.weight + " · " + entry.node.summary(), () -> {
                    this.editing = entry.node;
                    this.editingEntry = entry;
                    this.rowsDirty = true;
                }));
            }
        }).ifLeft(nodes -> {
            for (NodeDraft node : nodes) {
                this.rows.add(new ComponentGrid.ButtonRow(this.screen, this.font, node.summary(), () -> {
                    this.editing = node;
                    this.editingEntry = null;
                    this.rowsDirty = true;
                }));
            }
        });
        this.rows.add(new ComponentGrid.ButtonRow(this.screen, this.font,
                "+ " + eventLabel("add_node"), this::openAddNodeDropdown));
        if (this.frames.size() == 1) {
            this.rows.add(new ComponentGrid.ButtonRow(this.screen, this.font,
                    "- " + eventLabel("remove_group"), () -> {
                this.groups.remove(frame.title);
                this.frames.clear();
                this.editing = null;
                this.rowsDirty = true;
                this.markDirty();
            }));
        }
    }

    private void addNodeEditorRows() {
        NodeDraft node = this.editing;
        if (node == null) {
            this.rowsDirty = true;
            return;
        }
        this.rows.add(this.backRow());
        if (this.editingEntry != null) {
            RandomizeEntry entry = this.editingEntry;
            this.rows.add(new ComponentGrid.TextRow(this.screen, this.font,
                    () -> eventLabel("weight"), String.valueOf(entry.weight), text -> {
                try {
                    entry.weight = Math.max(1, Integer.parseInt(text.trim()));
                } catch (NumberFormatException ignored) {
                    entry.weight = 1;
                }
                this.markDirty();
            }));
        }
        switch (node) {
            case LogDraft log -> this.rows.add(new ComponentGrid.TextRow(this.screen, this.font,
                    () -> eventLabel("message"), log.message, text -> {
                log.message = text;
                this.markDirty();
            }));
            case ExpressionDraft exp -> {
                this.rows.add(new ComponentGrid.TextRow(this.screen, this.font,
                        () -> eventLabel("exp"), exp.exp, text -> {
                    exp.exp = text;
                    this.markDirty();
                }));
                this.rows.add(new ComponentGrid.ToggleRow(this.screen, this.font,
                        () -> eventLabel("log"), exp.log, value -> {
                    exp.log = value;
                    this.markDirty();
                }));
            }
            case SoundDraft sound -> this.rows.add(new ComponentGrid.TextRow(this.screen, this.font,
                    () -> eventLabel("sound_event"), sound.eventName, text -> {
                sound.eventName = text;
                this.markDirty();
            }));
            case ParticleEffectDraft effect -> {
                this.rows.add(new ComponentGrid.TextRow(this.screen, this.font,
                        () -> eventLabel("effect"), effect.effect, text -> {
                    effect.effect = text;
                    this.markDirty();
                }));
                this.rows.add(new ComponentGrid.SelectRow(this.screen, this.font,
                        () -> eventLabel("effect_type"),
                        java.util.Arrays.stream(ParticleEffect.Type.values()).map(ParticleEffect.Type::getSerializedName).toList(),
                        effect.type, index -> {
                    effect.type = index;
                    this.markDirty();
                }));
                this.rows.add(new ComponentGrid.TextRow(this.screen, this.font,
                        () -> eventLabel("pre_effect_expression"),
                        effect.preEffectExpression, text -> {
                    effect.preEffectExpression = text;
                    this.markDirty();
                }));
                // 共享变量：变量名一行、默认值一行（默认值为空 = 沿用父发射器里的变量）+ 删除按钮
                for (int i = 0; i < effect.sharedVars.size(); i++) {
                    int index = i;
                    SharedVarDraft shared = effect.sharedVars.get(index);
                    String nameLabel = eventLabel("shared_vars") + "[" + (index + 1) + "]";
                    this.rows.add(new ComponentGrid.TextRow(this.screen, this.font, () -> nameLabel,
                            shared.name, text -> {
                        shared.name = text.trim();
                        this.markDirty();
                    }));
                    String defaultLabel = eventLabel("shared_vars_default") + "[" + (index + 1) + "]";
                    this.rows.add(new ComponentGrid.TextRow(this.screen, this.font, () -> defaultLabel,
                            shared.defaultValue, text -> {
                        shared.defaultValue = text.trim();
                        this.markDirty();
                    }));
                    this.rows.add(new ComponentGrid.ButtonRow(this.screen, this.font,
                            "- " + eventLabel("shared_vars_remove"), () -> {
                        effect.sharedVars.remove(index);
                        this.rowsDirty = true;
                        this.markDirty();
                    }));
                }
                this.rows.add(new ComponentGrid.ButtonRow(this.screen, this.font,
                        "+ " + eventLabel("shared_vars_add"), () -> {
                    effect.sharedVars.add(new SharedVarDraft("", ""));
                    this.rowsDirty = true;
                    this.markDirty();
                }));
            }
            case WeightDraft weight -> this.rows.add(new ComponentGrid.TextRow(this.screen, this.font,
                    () -> eventLabel("weight"), String.valueOf(weight.value), text -> {
                try {
                    weight.value = Math.max(1, Integer.parseInt(text.trim()));
                } catch (NumberFormatException ignored) {
                    weight.value = 1;
                }
                this.markDirty();
            }));
            case SequenceDraft sequence -> this.rows.add(new ComponentGrid.ButtonRow(this.screen, this.font,
                    "> " + eventLabel("children") + " (" + sequence.children.size() + ")",
                    () -> {
                        this.frames.addLast(Frame.ofNodes(sequence.key, sequence.children));
                        this.editing = null;
                        this.editingEntry = null;
                        this.rowsDirty = true;
                    }));
            case RandomizeDraft randomize -> this.rows.add(new ComponentGrid.ButtonRow(this.screen, this.font,
                    "> " + eventLabel("children") + " (" + randomize.entries.size() + ")",
                    () -> {
                        this.frames.addLast(Frame.ofEntries(randomize.key, randomize.entries));
                        this.editing = null;
                        this.editingEntry = null;
                        this.rowsDirty = true;
                    }));
            default -> {
            }
        }
        this.rows.add(new ComponentGrid.ButtonRow(this.screen, this.font,
                "- " + eventLabel("remove_node"), () -> {
            this.deleteEditing();
            this.rowsDirty = true;
            this.markDirty();
        }));
    }

    private ComponentGrid.ButtonRow backRow() {
        return new ComponentGrid.ButtonRow(this.screen, this.font,
                "< " + Component.translatable("particle_editor.event.back").getString(), () -> {
            if (this.editing != null) {
                this.editing = null;
                this.editingEntry = null;
            } else if (this.frames.size() > 1) {
                this.frames.removeLast();
            } else {
                this.frames.clear();
            }
            this.rowsDirty = true;
        });
    }

    private void deleteEditing() {
        NodeDraft node = this.editing;
        if (node == null) {
            return;
        }
        if (this.editingEntry != null) {
            if (!this.frames.isEmpty()) {
                this.frames.getLast().either.ifRight(entries -> entries.remove(this.editingEntry));
            }
        } else if (!this.frames.isEmpty()) {
            this.frames.getLast().either.ifLeft(nodes -> nodes.remove(node));
        }
        this.editing = null;
        this.editingEntry = null;
    }

    /// 当前容器里还能添加的节点类型（见类注释里的规则）；空 `randomize` 条目容器允许任意非容器节点。
    /// 当前容器里还能添加的节点类型（见类注释里的规则）。
    ///
    /// - 容器里已经有 `sequence` / `randomize`：不能再加任何兄弟节点（只能进它们的子节点列表里加）；
    /// - 容器里已经有任意「叶子」节点（`log` / `expression` / `sound_effect` / `particle_effect`）：
    ///   只能再加**其它种类的叶子节点**（例如已有 log 时只剩 expression / sound_effect / particle_effect）；
    /// - 空容器：六种类型都可加；`randomize` 的每个条目是独立 map，不受同容器限制。
    private List<String> availableTypes(Frame frame) {
        return frame.either.map(nodes -> {
            if (nodes.stream().anyMatch(NodeDraft::isContainer)) {
                return List.of();
            }
            Set<String> present = new HashSet<>();
            for (NodeDraft node : nodes) {
                present.add(node.type());
            }
            boolean leafPresent = present.stream().anyMatch(LEAF_TYPES::contains);
            if (leafPresent) {
                return LEAF_TYPES.stream().filter(type -> !present.contains(type)).toList();
            }
            return NODE_TYPES.stream().filter(type -> !present.contains(type)).toList();
        }, ignored -> NODE_TYPES);
    }

    private void openAddNodeDropdown(int buttonTop) {
        this.addTypes = this.availableTypes(this.frames.getLast());
        List<String> shown = this.addTypes.isEmpty()
                ? List.of(Component.translatable("particle_editor.events.none_available").getString())
                : this.addTypes.stream().map(EventsTab::nodeTypeLabel).toList();
        this.addDropdown.setItems(shown);
        this.addDropdown.setPosition(2, buttonTop + ComponentGrid.ROW_HEIGHT);
        this.addDropdown.toggle();
    }

    private static String nodeTypeLabel(String type) {
        return Component.translatable("particle_editor.event_node." + type).getString();
    }

    private NodeDraft createNode(String type) {
        Frame frame = this.frames.getLast();
        NodeDraft node = switch (type) {
            case "expression" -> new ExpressionDraft("", false);
            case "sound_effect" -> new SoundDraft("minecraft:empty");
            case "particle_effect" -> new ParticleEffectDraft("minecraft:missing", ParticleEffect.Type.EMITTER.ordinal(), "", List.of());
            case "sequence" -> new SequenceDraft();
            case "randomize" -> new RandomizeDraft();
            case "weight" -> new WeightDraft();
            default -> new LogDraft("");
        };
        frame.either.ifRight(entries -> {
            RandomizeEntry entry = new RandomizeEntry();
            entry.node = node;
            entries.add(entry);
            this.editingEntry = entry;
        }).ifLeft(nodes -> {
            nodes.add(node);
            this.editingEntry = null;
        });
        return node;
    }

    // endregion

    // region 交互

    @Override
    boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.ensureLoaded();
        if (this.addDropdown.isOpen()) {
            int index = this.addDropdown.hitTest(mouseX, mouseY);
            this.addDropdown.close();
            if (index >= 0 && index < this.addTypes.size()) {
                this.editing = this.createNode(this.addTypes.get(index));
                this.rowsDirty = true;
                this.markDirty();
            }
            this.flushDirty();
            return true;
        }
        if (mouseX < 0 || mouseX >= this.screen.toolboxWidth() || mouseY < this.contentTop || mouseY >= this.screen.height - 2) {
            this.closeEditing();
            return false;
        }
        for (ComponentGrid.Row row : this.rows) {
            if (row.mouseClicked(mouseX, mouseY)) {
                this.flushDirty();
                return true;
            }
        }
        this.closeEditing();
        return false;
    }

    @Override
    boolean mouseDragged(double mouseX, double mouseY) {
        for (ComponentGrid.Row row : this.rows) {
            if (row.mouseDragged(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    boolean mouseReleased(double mouseX, double mouseY) {
        boolean handled = false;
        for (ComponentGrid.Row row : this.rows) {
            if (row.mouseReleased(mouseX, mouseY)) {
                handled = true;
            }
        }
        return handled;
    }

    @Override
    boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean handled = false;
        for (ComponentGrid.Row row : this.rows) {
            if (row.keyPressed(keyCode, scanCode, modifiers)) {
                handled = true;
                break;
            }
        }
        this.flushDirty();
        return handled;
    }

    @Override
    boolean charTyped(char codePoint, int modifiers) {
        for (ComponentGrid.Row row : this.rows) {
            if (row.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    boolean isCapturingInput() {
        if (this.addDropdown.isOpen()) {
            return true;
        }
        for (ComponentGrid.Row row : this.rows) {
            if (row.isCapturingInput()) {
                return true;
            }
        }
        return false;
    }

    @Override
    boolean scroll(double mouseX, double mouseY, double scrollY) {
        if (this.addDropdown.contains(mouseX, mouseY)) {
            this.addDropdown.scroll(scrollY);
            return true;
        }
        if (mouseX < 0 || mouseX >= this.screen.toolboxWidth() || mouseY < this.contentTop || mouseY >= this.screen.height - 2) {
            return false;
        }
        for (ComponentGrid.Row row : this.rows) {
            if (row.scroll(mouseX, mouseY, scrollY)) {
                return true;
            }
        }
        int contentHeight = 0;
        for (ComponentGrid.Row row : this.rows) {
            contentHeight += row.height();
        }
        int maxScroll = Math.max(0, contentHeight - Math.max(1, this.screen.height - 2 - this.contentTop));
        this.scroll = net.minecraft.util.Mth.clamp(this.scroll - (int) (scrollY * 12), 0, maxScroll);
        return true;
    }

    @Override
    void closeEditing() {
        this.addDropdown.close();
        for (ComponentGrid.Row row : this.rows) {
            row.closeEditing();
        }
    }

    @Override
    void onEffectReplaced() {
        this.loadedFrom = null;
        this.groups.clear();
        this.frames.clear();
        this.editing = null;
        this.editingEntry = null;
        this.rowsDirty = true;
        this.dirty = false;
        this.closeEditing();
    }

    private static String eventLabel(String key) {
        return Component.translatable("particle_editor.event." + key).getString();
    }

    // endregion
}

package org.mesdag.particlestorm.editor;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.data.curve.CurveNode;
import org.mesdag.particlestorm.data.curve.CurveNodes;
import org.mesdag.particlestorm.data.curve.CurveType;
import org.mesdag.particlestorm.data.curve.ParticleCurve;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// 「曲线」页签：编辑 `DefinedParticleEffect#curves`（曲线名 → `ParticleCurve`）。
///
/// 两级视图：
/// 1. **曲线列表**：第一行「新曲线」文本行（`ENTER` 创建默认曲线），每行显示 `名字 (类型)`，点击进入编辑器；
/// 2. **曲线编辑器**：`type` 下拉（CATMULL_ROM / LINEAR / BEZIER / BEZIER_CHAIN）+ `input` / `horizontal_range`
///    两个 molang 文本行 + 节点列表：
///    - `BEZIER_CHAIN`：节点是「时间 → (value, slope)」，每个节点一个三行向量行（时间 / value / slope）；
///    - 其它类型：节点是一组 molang 表达式，每个节点一行文本；
///    节点下方 `+` 添加、每条节点自带 `-` 删除；底部可删除整条曲线。
///
/// 数据是草稿：载入时从 `effect.curves` 深拷贝，改动后整体重建 `curves` map → 替换 `screen.effect` → 清场景重新发射；
/// `BEZIER_CHAIN` 与其它类型的节点表示不同，切类型时会自动把另一边的数据补齐（节点值互转），不会出现空节点列表。
final class CurvesTab extends EditorTab {
    private static final class CurveDraft {
        String name;
        CurveType type;
        String input;
        String horizontalRange;
        /// 非 BEZIER_CHAIN 的节点（molang 表达式文本）。
        final List<String> values = new ArrayList<>();
        /// BEZIER_CHAIN 的节点：`[时间, value, slope]`。
        final List<String[]> nodes = new ArrayList<>();
        /// 预览里选中的节点下标（-1 = 没选）。
        int selected = -1;

        CurveDraft(String name, CurveType type, String input, String horizontalRange) {
            this.name = name;
            this.type = type;
            this.input = input;
            this.horizontalRange = horizontalRange;
        }

        /// 用节点表的 value 列同步 molang 表达式列表（`BEZIER_CHAIN` 与其它类型互转时用）。
        void syncValuesFromNodes() {
            this.values.clear();
            for (String[] node : this.nodes) {
                this.values.add(node[1]);
            }
        }

        /// 两种节点表示（索引序列 / 时间-斜率序列）互转，保证切类型后节点列表不会空着。
        void normalize() {
            if (this.type == CurveType.BEZIER_CHAIN) {
                if (this.nodes.isEmpty() && !this.values.isEmpty()) {
                    for (int i = 0; i < this.values.size(); i++) {
                        this.nodes.add(new String[]{Float.toString(i), this.values.get(i), "1.0"});
                    }
                }
            } else if (this.values.isEmpty() && !this.nodes.isEmpty()) {
                for (String[] node : this.nodes) {
                    this.values.add(node[1]);
                }
            }
        }

        ParticleCurve build() {
            this.normalize();
            CurveNodes curveNodes;
            if (this.type == CurveType.BEZIER_CHAIN) {
                Map<String, CurveNode> map = new LinkedHashMap<>();
                for (String[] node : this.nodes) {
                    String key = Float.toString(parseFloat(node[0], 0.0F));
                    map.put(key, new CurveNode(parseFloat(node[1], 1.0F), parseFloat(node[2], 1.0F)));
                }
                curveNodes = new CurveNodes(Either.left(map));
            } else {
                List<FloatMolangExp> list = new ArrayList<>();
                for (String value : this.values) {
                    list.add(ComponentGrid.expOf(value));
                }
                curveNodes = new CurveNodes(Either.right(list));
            }
            return new ParticleCurve(this.type, ComponentGrid.expOf(this.input),
                    ComponentGrid.expOf(this.horizontalRange), curveNodes);
        }
    }

    private final Map<String, CurveDraft> curves = new LinkedHashMap<>();
    private final List<ComponentGrid.Row> rows = new ArrayList<>();
    @Nullable
    private CurveDraft editing;
    private boolean rowsDirty = true;
    /// 预览里选中/拖动节点后**等鼠标松开**再重建行：拖动期间重建会把 PreviewRow 换成新实例、丢拖拽状态。
    private boolean pendingRebuild;
    private boolean dirty;
    @Nullable
    private DefinedParticleEffect loadedFrom;

    CurvesTab(EditorScreen screen, Font font) {
        super(screen, font);
    }

    @Override
    Component title() {
        return Component.translatable("particle_editor.tab.curves");
    }

    // region 载入 / 同步

    private void ensureLoaded() {
        DefinedParticleEffect effect = this.screen.effect;
        if (effect == this.loadedFrom) {
            return;
        }
        this.loadedFrom = effect;
        this.curves.clear();
        this.editing = null;
        this.dirty = false;
        this.rowsDirty = true;
        if (effect == null) {
            return;
        }
        effect.curves.forEach((name, curve) -> this.curves.put(name, fromCurve(name, curve)));
    }

    private static CurveDraft fromCurve(String name, ParticleCurve curve) {
        CurveDraft draft = new CurveDraft(name, curve.type, expText(curve.input), expText(curve.horizontalRange));
        if (curve.nodes.either.left().isPresent()) {
            for (Map.Entry<String, CurveNode> entry : curve.nodes.either.left().get().entrySet()) {
                CurveNode node = entry.getValue();
                draft.nodes.add(new String[]{entry.getKey(), String.valueOf(node.value()), String.valueOf(node.slope())});
                draft.values.add(String.valueOf(node.value()));
            }
        } else {
            List<FloatMolangExp> list = curve.nodes.either.right().orElse(List.of());
            for (int i = 0; i < list.size(); i++) {
                String text = expText(list.get(i));
                draft.values.add(text);
                draft.nodes.add(new String[]{Float.toString(i), text, "1.0"});
            }
        }
        return draft;
    }

    private static String expText(FloatMolangExp exp) {
        return exp.getExpStr().isEmpty() ? String.valueOf(exp.getConstant()) : exp.getExpStr();
    }

    private void apply() {
        this.dirty = false;
        DefinedParticleEffect effect = this.screen.effect;
        if (effect == null) {
            return;
        }
        try {
            Map<String, ParticleCurve> curves = new LinkedHashMap<>();
            for (CurveDraft draft : this.curves.values()) {
                curves.put(draft.name, draft.build());
            }
            this.screen.effect = new DefinedParticleEffect(effect.description, curves, effect.components, effect.events);
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
        int width = this.screen.toolboxWidth();
        int viewBottom = this.screen.height - 2;
        int contentHeight = 0;
        for (ComponentGrid.Row row : this.rows) {
            contentHeight += row.height();
        }
        int maxScroll = Math.max(0, contentHeight - Math.max(1, viewBottom - contentTop));
        this.scroll = net.minecraft.util.Mth.clamp(this.scroll, 0, maxScroll);
        guiGraphics.enableScissor(0, contentTop, width, viewBottom);
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
        } finally {
            guiGraphics.disableScissor();
        }
    }

    private void rebuildRows() {
        this.rowsDirty = false;
        this.rows.clear();
        if (this.editing == null) {
            this.addListRows();
        } else {
            this.addEditorRows(this.editing);
        }
    }

    private void addListRows() {
        this.rows.add(new ComponentGrid.TextRow(this.screen, this.font,
                () -> curveLabel("new"), "", text -> {
            String name = text.trim();
            if (!name.isEmpty() && !this.curves.containsKey(name)) {
                CurveDraft draft = new CurveDraft(name, CurveType.LINEAR, "v.particle_age", "v.particle_lifetime");
                draft.values.add("0");
                draft.values.add("1");
                draft.values.add("0");
                this.curves.put(name, draft);
                this.rowsDirty = true;
                this.markDirty();
            }
        }));
        for (CurveDraft draft : this.curves.values()) {
            this.rows.add(new ComponentGrid.ButtonRow(this.screen, this.font,
                    draft.name + " (" + curveTypeLabel(draft.type) + ")", () -> {
                this.editing = draft;
                this.rowsDirty = true;
            }));
        }
    }

    private void addEditorRows(CurveDraft draft) {
        this.rows.add(new ComponentGrid.ButtonRow(this.screen, this.font,
                "< " + Component.translatable("particle_editor.event.back").getString(), () -> {
            this.editing = null;
            this.rowsDirty = true;
        }));
        // 曲线名可以改（它就是输出变量的名字）
        this.rows.add(new ComponentGrid.TextRow(this.screen, this.font,
                () -> curveLabel("name"), draft.name, text -> this.renameCurve(draft, text)));
        this.rows.add(new ComponentGrid.SelectRow(this.screen, this.font,
                () -> curveLabel("type"),
                java.util.Arrays.stream(CurveType.values()).map(CurvesTab::curveTypeLabel).toList(),
                draft.type.ordinal(), index -> {
            draft.type = CurveType.values()[index];
            draft.normalize();
            this.rowsDirty = true;
            this.markDirty();
        }));
        this.rows.add(new ComponentGrid.TextRow(this.screen, this.font,
                () -> curveLabel("input"), draft.input, text -> {
            draft.input = text;
            this.markDirty();
        }));
        this.rows.add(new ComponentGrid.TextRow(this.screen, this.font,
                () -> curveLabel("horizontal_range"), draft.horizontalRange, text -> {
            draft.horizontalRange = text;
            this.markDirty();
        }));
        // 可交互预览：点击空白添加节点、点节点选中、按住拖动改值（BEZIER_CHAIN 还能左右拖时间）
        this.rows.add(new PreviewRow(this, draft));
        int selected = draft.selected;
        if (draft.type == CurveType.BEZIER_CHAIN) {
            if (selected >= 0 && selected < draft.nodes.size()) {
                final int sel = selected;
                this.rows.add(new ComponentGrid.VectorRow(this.screen, this.font,
                        () -> curveLabel("node") + " " + (sel + 1), 3, draft.nodes.get(sel), parts -> {
                    draft.nodes.set(sel, parts);
                    draft.syncValuesFromNodes();
                    this.markDirty();
                }));
                this.rows.add(new ComponentGrid.ButtonRow(this.screen, this.font,
                        "- " + curveLabel("node_remove"), () -> {
                    draft.nodes.remove(sel);
                    draft.selected = Math.min(sel, draft.nodes.size() - 1);
                    draft.syncValuesFromNodes();
                    this.rowsDirty = true;
                    this.markDirty();
                }));
            }
        } else {
            if (selected >= 0 && selected < draft.values.size()) {
                final int sel = selected;
                this.rows.add(new ComponentGrid.TextRow(this.screen, this.font,
                        () -> curveLabel("node_value"), draft.values.get(sel), text -> {
                    draft.values.set(sel, text);
                    this.markDirty();
                }));
                this.rows.add(new ComponentGrid.ButtonRow(this.screen, this.font,
                        "- " + curveLabel("node_remove"), () -> {
                    draft.values.remove(sel);
                    draft.selected = Math.min(sel, draft.values.size() - 1);
                    draft.normalize();
                    this.rowsDirty = true;
                    this.markDirty();
                }));
            }
        }
        if (draft.selected < 0) {
            this.rows.add(new ComponentGrid.InfoRow(this.screen, this.font, () -> "",
                    curveLabel("hint")));
        }
        this.rows.add(new ComponentGrid.ButtonRow(this.screen, this.font,
                "- " + curveLabel("remove"), () -> {
            this.curves.remove(draft.name);
            this.editing = null;
            this.rowsDirty = true;
            this.markDirty();
        }));
    }

    /// 重命名曲线（输出变量名）：换 map 的 key，保持顺序不变。
    private void renameCurve(CurveDraft draft, String text) {
        String name = text.trim();
        if (name.isEmpty() || name.equals(draft.name)) {
            return;
        }
        if (this.curves.containsKey(name)) {
            this.screen.error = Component.literal("Duplicate curve name: " + name);
            return;
        }
        Map<String, CurveDraft> renamed = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, CurveDraft> entry : this.curves.entrySet()) {
            renamed.put(entry.getValue() == draft ? name : entry.getKey(), entry.getValue());
        }
        this.curves.clear();
        this.curves.putAll(renamed);
        draft.name = name;
        this.rowsDirty = true;
        this.markDirty();
    }

    // endregion

    // region 交互

    @Override
    boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.ensureLoaded();
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
        if (this.pendingRebuild) {
            this.pendingRebuild = false;
            this.rowsDirty = true;
        }
        // 拖节点改值只在松手时重建行；这里顺便把草稿落地（`markDirty` 之后通常要等下一次点击/按键才 apply，
        // 否则拖完松手预览里既不重发粒子、曲线变量也不生效）
        this.flushDirty();
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
    boolean scroll(double mouseX, double mouseY, double scrollY) {
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
    boolean isCapturingInput() {
        for (ComponentGrid.Row row : this.rows) {
            if (row.isCapturingInput()) {
                return true;
            }
        }
        return false;
    }

    @Override
    void closeEditing() {
        for (ComponentGrid.Row row : this.rows) {
            row.closeEditing();
        }
    }

    @Override
    void onEffectReplaced() {
        this.loadedFrom = null;
        this.curves.clear();
        this.editing = null;
        this.rowsDirty = true;
        this.dirty = false;
        this.closeEditing();
    }

    // region 预览采样

    /// 曲线类型的显示名（`particle_editor.curve_type.*`）。
    private static String curveTypeLabel(CurveType type) {
        return Component.translatable("particle_editor.curve_type." + type.getSerializedName()).getString();
    }

    /// 按当前草稿采样：`t` 在 [0,1]；节点里有无法求值的表达式时返回 `NaN`（预览跳过该点）。
    private static float sample(CurveDraft draft, float t) {
        draft.normalize();
        if (draft.type == CurveType.BEZIER_CHAIN) {
            if (draft.nodes.isEmpty()) {
                return Float.NaN;
            }
            List<float[]> parsed = new ArrayList<>();
            for (String[] node : draft.nodes) {
                float time = parseFloat(node[0], Float.NaN);
                float value = parseFloat(node[1], Float.NaN);
                float slope = parseFloat(node[2], 1.0F);
                if (Float.isNaN(time) || Float.isNaN(value)) {
                    return Float.NaN;
                }
                parsed.add(new float[]{time, value, slope});
            }
            parsed.sort(java.util.Comparator.comparingDouble(node -> node[0]));
            float prevTime = 0.0F;
            float prevValue = 0.0F;
            float prevSlope = 0.0F;
            float nextTime = 1.0F;
            float nextValue = 0.0F;
            float nextSlope = 0.0F;
            boolean found = false;
            for (float[] node : parsed) {
                if (node[0] > t) {
                    nextTime = node[0];
                    nextValue = node[1];
                    nextSlope = node[2];
                    found = true;
                    break;
                }
                prevTime = node[0];
                prevValue = node[1];
                prevSlope = node[2];
            }
            if (!found) {
                nextValue = 0.0F;
                nextSlope = 0.0F;
            }
            float span = nextTime - prevTime;
            if (span <= 0.0F) {
                return prevValue;
            }
            return cubicBezier((t - prevTime) / span, prevValue, prevValue + prevSlope / 3.0F,
                    nextValue - nextSlope / 3.0F, nextValue);
        }
        List<Float> values = new ArrayList<>();
        for (String text : draft.values) {
            float value = parseFloat(text, Float.NaN);
            if (Float.isNaN(value)) {
                return Float.NaN;
            }
            values.add(value);
        }
        if (values.isEmpty()) {
            return Float.NaN;
        }
        if (values.size() == 1) {
            return values.get(0);
        }
        return switch (draft.type) {
            case LINEAR -> {
                float scaled = t * (values.size() - 1);
                int index = Math.min((int) scaled, values.size() - 2);
                float factor = scaled - index;
                yield values.get(index) + (values.get(index + 1) - values.get(index)) * factor;
            }
            case CATMULL_ROM -> {
                int c = values.size() - 3;
                if (c <= 0) {
                    yield values.get(0);
                }
                float u = (1.0F + t * c) / (c + 2.0F);
                float scaled = u * (values.size() - 1);
                int index = Math.min(Math.max((int) scaled, 0), values.size() - 2);
                float factor = scaled - index;
                float p0 = values.get(Math.max(index - 1, 0));
                float p1 = values.get(index);
                float p2 = values.get(Math.min(index + 1, values.size() - 1));
                float p3 = values.get(Math.min(index + 2, values.size() - 1));
                yield catmullRom(p0, p1, p2, p3, factor);
            }
            default -> deCasteljau(values, t);
        };
    }

    private static float cubicBezier(float t, float v0, float v1, float v2, float v3) {
        float one = 1.0F - t;
        return one * one * one * v0 + 3.0F * one * one * t * v1 + 3.0F * one * t * t * v2 + t * t * t * v3;
    }

    private static float catmullRom(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return 0.5F * ((2.0F * p1) + (-p0 + p2) * t + (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * t2
                + (-p0 + 3.0F * p1 - 3.0F * p2 + p3) * t3);
    }

    private static float deCasteljau(List<Float> points, float t) {
        List<Float> current = new ArrayList<>(points);
        while (current.size() > 1) {
            List<Float> next = new ArrayList<>(current.size() - 1);
            for (int i = 0; i < current.size() - 1; i++) {
                next.add(current.get(i) + (current.get(i + 1) - current.get(i)) * t);
            }
            current = next;
        }
        return current.get(0);
    }

    /// 曲线预览行：把当前草稿按类型采样成折线画出来（y 值裁剪到 [0,1]）。
    private static final class PreviewRow extends ComponentGrid.Row {
        private final CurvesTab tab;
        private final CurveDraft draft;
        private int graphX;
        private int graphY;
        private int graphWidth;
        private int graphHeight;
        private int dragNode = -1;
        /// 正在拖的是节点本身（false）还是它的斜率手柄（true）。
        private boolean draggingSlope;

        PreviewRow(CurvesTab tab, CurveDraft draft) {
            super(() -> "");
            this.tab = tab;
            this.draft = draft;
        }

        @Override
        int height() {
            return 72;
        }

        @Override
        void render(GuiGraphics guiGraphics, int top, int mouseX, int mouseY) {
            this.graphX = 2;
            this.graphY = top + 2;
            this.graphWidth = this.tab.screen.toolboxWidth() - 4;
            this.graphHeight = 60;
            int x = this.graphX;
            int y = this.graphY;
            int w = this.graphWidth;
            int h = this.graphHeight;
            guiGraphics.fill(x, y, x + w, y + h, 0xFF101018);
            guiGraphics.renderOutline(x, y, w, h, 0xFF4A4A58);
            for (int i = 1; i < 4; i++) {
                int gridX = x + w * i / 4;
                guiGraphics.fill(gridX, y + 1, gridX + 1, y + h - 1, 0x30FFFFFF);
            }
            int mid = y + h / 2;
            guiGraphics.fill(x + 1, mid, x + w - 1, mid + 1, 0x30FFFFFF);
            int samples = Math.max(2, w - 2);
            int previousY = Integer.MIN_VALUE;
            for (int i = 0; i < samples; i++) {
                float t = i / (float) (samples - 1);
                float value = CurvesTab.sample(this.draft, t);
                if (Float.isNaN(value)) {
                    previousY = Integer.MIN_VALUE;
                    continue;
                }
                int pixelY = y + h - 1 - Math.round(net.minecraft.util.Mth.clamp(value, 0.0F, 1.0F) * (h - 2));
                int columnX = x + 1 + i;
                if (previousY == Integer.MIN_VALUE) {
                    guiGraphics.fill(columnX, pixelY, columnX + 1, pixelY + 1, 0xFF9CDCFE);
                } else {
                    int from = Math.min(previousY, pixelY);
                    int to = Math.max(previousY, pixelY);
                    guiGraphics.fill(columnX, from, columnX + 1, to + 1, 0xFF9CDCFE);
                }
                previousY = pixelY;
            }
            // 节点标记（选中节点白色高亮）
            this.draft.normalize();
            int count = this.nodeCount();
            for (int i = 0; i < count; i++) {
                int centerX = this.nodeScreenX(i);
                int centerY = this.nodeScreenY(i);
                if (centerX == Integer.MIN_VALUE) {
                    continue;
                }
                boolean selected = i == this.draft.selected;
                // BEZIER_CHAIN：画斜率手柄（从节点出发的切线方向），拖手柄改 slope
                if (this.draft.type == CurveType.BEZIER_CHAIN) {
                    int endX = this.handleEndX(i);
                    int endY = this.handleEndY(i);
                    if (endX != Integer.MIN_VALUE) {
                        this.drawLine(guiGraphics, centerX, centerY, endX, endY, selected ? 0xFFDCDC9C : 0x80DCDC9C);
                        guiGraphics.fill(endX - 1, endY - 1, endX + 2, endY + 2, selected ? 0xFFFFFFFF : 0xFF9C9C6C);
                    }
                }
                guiGraphics.fill(centerX - 2, centerY - 2, centerX + 3, centerY + 3, selected ? 0xFFFFFFFF : 0xFF7F7F7F);
                guiGraphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, 0xFFB5CEA8);
            }
        }

        private void drawLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
            int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
            for (int i = 0; i <= steps; i++) {
                int px = x1 + Math.round((x2 - x1) * (steps == 0 ? 0.0F : i / (float) steps));
                int py = y1 + Math.round((y2 - y1) * (steps == 0 ? 0.0F : i / (float) steps));
                guiGraphics.fill(px, py, px + 1, py + 1, color);
            }
        }

        private static final int HANDLE_LENGTH = 18;

        private int handleEndX(int index) {
            int centerX = this.nodeScreenX(index);
            if (centerX == Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            float slope = parseFloat(this.draft.nodes.get(index)[2], 1.0F);
            float length = (float) Math.sqrt(1.0F + slope * slope);
            return centerX + Math.round(HANDLE_LENGTH / length);
        }

        private int handleEndY(int index) {
            int centerX = this.nodeScreenX(index);
            int centerY = this.nodeScreenY(index);
            if (centerX == Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }
            float slope = parseFloat(this.draft.nodes.get(index)[2], 1.0F);
            float length = (float) Math.sqrt(1.0F + slope * slope);
            return centerY - Math.round(HANDLE_LENGTH * slope / length);
        }

        /// 鼠标相对节点的位置换算成斜率（屏幕 y 向上为正，链式斜率就是 value 对归一化时间的导数）。
        private float slopeAt(int index, double mouseX, double mouseY) {
            int centerX = this.nodeScreenX(index);
            int centerY = this.nodeScreenY(index);
            if (centerX == Integer.MIN_VALUE) {
                return parseFloat(this.draft.nodes.get(index)[2], 1.0F);
            }
            float normalizedTime = (float) ((mouseX - centerX) / (double) Math.max(1, this.graphWidth - 2));
            float normalizedValue = (float) ((centerY - mouseY) / (double) Math.max(1, this.graphHeight - 2));
            if (Math.abs(normalizedTime) < 1.0E-4F) {
                return Math.signum(normalizedValue == 0.0F ? 1.0F : normalizedValue) * 50.0F;
            }
            return net.minecraft.util.Mth.clamp(normalizedValue / normalizedTime, -50.0F, 50.0F);
        }

        private int handleAt(double mouseX, double mouseY) {
            for (int i = 0; i < this.draft.nodes.size(); i++) {
                int endX = this.handleEndX(i);
                int endY = this.handleEndY(i);
                if (endX == Integer.MIN_VALUE) {
                    continue;
                }
                int dx = (int) Math.round(mouseX) - endX;
                int dy = (int) Math.round(mouseY) - endY;
                if (dx * dx + dy * dy <= 25) {
                    return i;
                }
            }
            return -1;
        }

        private int nodeCount() {
            return this.draft.type == CurveType.BEZIER_CHAIN ? this.draft.nodes.size() : this.draft.values.size();
        }

        private int nodeScreenX(int index) {
            if (this.draft.type == CurveType.BEZIER_CHAIN) {
                float time = parseFloat(this.draft.nodes.get(index)[0], Float.NaN);
                if (Float.isNaN(time)) {
                    return Integer.MIN_VALUE;
                }
                return this.graphX + 1 + Math.round(net.minecraft.util.Mth.clamp(time, 0.0F, 1.0F) * (this.graphWidth - 2));
            }
            int n = this.draft.values.size();
            float t = n <= 1 ? 0.5F : index / (float) (n - 1);
            return this.graphX + 1 + Math.round(t * (this.graphWidth - 2));
        }

        private int nodeScreenY(int index) {
            float value = this.draft.type == CurveType.BEZIER_CHAIN
                    ? parseFloat(this.draft.nodes.get(index)[1], Float.NaN)
                    : parseFloat(this.draft.values.get(index), Float.NaN);
            if (Float.isNaN(value)) {
                return Integer.MIN_VALUE;
            }
            return this.graphY + this.graphHeight - 1 - Math.round(net.minecraft.util.Mth.clamp(value, 0.0F, 1.0F) * (this.graphHeight - 2));
        }

        private boolean inGraph(double mouseX, double mouseY) {
            return mouseX >= this.graphX && mouseX < this.graphX + this.graphWidth
                    && mouseY >= this.graphY && mouseY < this.graphY + this.graphHeight;
        }

        private float graphT(double mouseX) {
            return net.minecraft.util.Mth.clamp((float) ((mouseX - this.graphX - 1) / (float) (this.graphWidth - 2)), 0.0F, 1.0F);
        }

        private float graphValue(double mouseY) {
            return net.minecraft.util.Mth.clamp((float) ((this.graphY + this.graphHeight - 1 - mouseY) / (float) (this.graphHeight - 2)), 0.0F, 1.0F);
        }

        private int nodeAt(double mouseX, double mouseY) {
            int best = -1;
            int bestDistance = 81;
            for (int i = 0; i < this.nodeCount(); i++) {
                int centerX = this.nodeScreenX(i);
                int centerY = this.nodeScreenY(i);
                if (centerX == Integer.MIN_VALUE) {
                    continue;
                }
                int dx = (int) Math.round(mouseX) - centerX;
                int dy = (int) Math.round(mouseY) - centerY;
                int distance = dx * dx + dy * dy;
                if (distance <= bestDistance) {
                    bestDistance = distance;
                    best = i;
                }
            }
            return best;
        }

        /// BEZIER_CHAIN 拖时间时不能越过相邻节点（键会重复）。
        private float[] timeBounds(int index) {
            float current = parseFloat(this.draft.nodes.get(index)[0], 0.5F);
            float lower = 0.0F;
            float upper = 1.0F;
            for (int i = 0; i < this.draft.nodes.size(); i++) {
                if (i == index) {
                    continue;
                }
                float time = parseFloat(this.draft.nodes.get(i)[0], Float.NaN);
                if (Float.isNaN(time)) {
                    continue;
                }
                if (time < current) {
                    lower = Math.max(lower, time + 0.001F);
                } else {
                    upper = Math.min(upper, time - 0.001F);
                }
            }
            if (lower > upper) {
                lower = upper = Math.max(0.0F, Math.min(1.0F, (lower + upper) * 0.5F));
            }
            return new float[]{lower, upper};
        }

        @Override
        boolean mouseClicked(double mouseX, double mouseY) {
            if (!this.inGraph(mouseX, mouseY)) {
                return false;
            }
            if (this.draft.type == CurveType.BEZIER_CHAIN) {
                int handle = this.handleAt(mouseX, mouseY);
                if (handle >= 0) {
                    this.draft.selected = handle;
                    this.dragNode = handle;
                    this.draggingSlope = true;
                    this.tab.pendingRebuild = true;
                    this.tab.markDirty();
                    return true;
                }
            }
            int hit = this.nodeAt(mouseX, mouseY);
            if (hit >= 0) {
                this.draft.selected = hit;
                this.dragNode = hit;
                this.draggingSlope = false;
                this.tab.pendingRebuild = true;
                this.tab.markDirty();
                return true;
            }
            // 空白处点击：加节点
            float time = this.graphT(mouseX);
            float value = this.graphValue(mouseY);
            if (this.draft.type == CurveType.BEZIER_CHAIN) {
                this.draft.normalize();
                this.draft.nodes.add(new String[]{Float.toString(time), Float.toString(value), "1.0"});
                this.draft.syncValuesFromNodes();
                this.draft.selected = this.draft.nodes.size() - 1;
            } else {
                this.draft.normalize();
                int n = this.draft.values.size();
                int index = n <= 1 ? n : Math.max(1, Math.min(n - 1, Math.round(time * (n - 1))));
                this.draft.values.add(index, Float.toString(value));
                this.draft.selected = index;
            }
            this.draggingSlope = false;
            this.tab.pendingRebuild = true;
            this.tab.markDirty();
            return true;
        }

        @Override
        boolean mouseDragged(double mouseX, double mouseY) {
            if (this.dragNode < 0 || this.dragNode >= this.nodeCount()) {
                return false;
            }
            if (this.draft.type == CurveType.BEZIER_CHAIN && this.draggingSlope) {
                String[] node = this.draft.nodes.get(this.dragNode);
                float slope = this.slopeAt(this.dragNode, mouseX, mouseY);
                this.draft.nodes.set(this.dragNode, new String[]{node[0], node[1], Float.toString(slope)});
                this.tab.markDirty();
                return true;
            }
            float value = this.graphValue(mouseY);
            if (this.draft.type == CurveType.BEZIER_CHAIN) {
                float[] bounds = this.timeBounds(this.dragNode);
                float time = net.minecraft.util.Mth.clamp(this.graphT(mouseX), bounds[0], bounds[1]);
                String[] node = this.draft.nodes.get(this.dragNode);
                this.draft.nodes.set(this.dragNode, new String[]{Float.toString(time), Float.toString(value), node[2]});
                this.draft.syncValuesFromNodes();
            } else {
                this.draft.values.set(this.dragNode, Float.toString(value));
            }
            this.tab.markDirty();
            return true;
        }

        @Override
        boolean mouseReleased(double mouseX, double mouseY) {
            boolean wasDragging = this.dragNode >= 0;
            this.dragNode = -1;
            this.draggingSlope = false;
            if (wasDragging) {
                // 拖完（松开时）再重建行，让下方文本框同步拖动后的值
                this.tab.pendingRebuild = true;
            }
            return wasDragging;
        }
    }

    // endregion

    private static float parseFloat(String text, float fallback) {
        try {
            return Float.parseFloat(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String curveLabel(String key) {
        return Component.translatable("particle_editor.curve." + key).getString();
    }

    // endregion
}

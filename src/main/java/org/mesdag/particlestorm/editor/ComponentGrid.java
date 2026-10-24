package org.mesdag.particlestorm.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.IComponent;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.FloatMolangExp2;
import org.mesdag.particlestorm.data.molang.FloatMolangExp3;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/// 组件编辑 Grid：每个组件一个，把它的字段列成「标签 + 编辑控件」行。
///
/// 行由子类在构造时通过 [#text] / [#vector] / [#pair] / [#select] / [#toggle] / [#info] 添加：
/// - [TextRow]：点击整行打开原版 [EditBox]，`ENTER` 提交、`ESC` 取消；
/// - [SelectRow]：点击打开 [DropdownWidget]，选中即提交；
/// - [ToggleRow]：点击翻转；
/// - [InfoRow]：只读展示。
///
/// 行的提交回调把值存到 Grid 自己的字段里并 [#markChanged]；宿主（[ComponentsTab]）消费后调用 [#build]
/// 用当前字段 **new 一个新组件** 替换 `effect.components` 里的旧实例并重新发射，不经过 codec。
public abstract class ComponentGrid {
    static final int ROW_HEIGHT = 24;
    /// 标签在上、值在下：标签行 y 偏移、值行 y 偏移与值缩进。
    static final int LABEL_Y = 2;
    static final int VALUE_Y = 13;
    static final int VALUE_X = 12;

    protected final EditorScreen screen;
    protected final Font font;
    private final List<Row> rows = new ArrayList<>();
    private @Nullable ResourceLocation componentId;
    private boolean changed;

    protected ComponentGrid(EditorScreen screen, Font font) {
        this.screen = screen;
        this.font = font;
    }

    /// 当前编辑的组件 id（由宿主在打开/新建时设置，用于行标签的翻译键）。
    void setComponentId(@Nullable ResourceLocation componentId) {
        this.componentId = componentId;
    }

    /// 行标签的翻译键：`particlestorm.editor.<namespace>.<path>.<field>`（未翻译时原样显示键）。
    protected String fieldLabel(String field) {
        ResourceLocation id = this.componentId;
        if (id == null) {
            return field;
        }
        return Component.translatable("particle_editor." + id.getNamespace() + "." + id.getPath() + "." + field).getString();
    }

    /// 构造结果：组件实例 + 它对应的 id（同族子类型不同，id 不同）。
    public record BuiltComponent(ResourceLocation id, IComponent component) {
    }

    /// 新建默认实例（「添加组件」用）。
    public abstract BuiltComponent createDefault();

    /// 用当前字段构造新组件实例（含子类型 id）；解析/校验失败时抛异常（由宿主显示错误、保留旧组件）。
    public abstract BuiltComponent build();

    /// 把现有组件载入各行（子类覆盖）。
    public void load(IComponent component) {
    }

    /// 是否允许从「添加组件」里新建（只读 Grid 返回 false）。
    public boolean canAdd() {
        return true;
    }

    /// 场景内 3D 手柄：组件有可拖拽的几何表示时覆盖它，Grid 打开期间 [EditorScreen] 会在场景里绘制并转发鼠标。
    @Nullable
    SceneGizmo sceneGizmo() {
        return null;
    }

    // region 行构造

    TextRow text(String label, String value, Consumer<String> commit) {
        TextRow row = new TextRow(this.screen, this.font, () -> this.fieldLabel(label), value, commit);
        this.rows.add(row);
        return row;
    }

    /// 同上，标签由调用方动态给（多条目 Grid 用下标前缀区分，不走翻译键）。
    TextRow text(Supplier<String> label, String value, Consumer<String> commit) {
        TextRow row = new TextRow(this.screen, this.font, label, value, commit);
        this.rows.add(row);
        return row;
    }

    VectorRow vector(String label, FloatMolangExp3 value, Consumer<FloatMolangExp3> commit) {
        VectorRow row = new VectorRow(this.screen, this.font, () -> this.fieldLabel(label), 3,
                new String[]{expText(value.exp1()), expText(value.exp2()), expText(value.exp3())},
                parts -> commit.accept(new FloatMolangExp3(expOf(parts[0]), expOf(parts[1]), expOf(parts[2]))));
        this.rows.add(row);
        return row;
    }

    /// 同上，标签由调用方动态给。
    VectorRow vector(Supplier<String> label, FloatMolangExp3 value, Consumer<FloatMolangExp3> commit) {
        VectorRow row = new VectorRow(this.screen, this.font, label, 3,
                new String[]{expText(value.exp1()), expText(value.exp2()), expText(value.exp3())},
                parts -> commit.accept(new FloatMolangExp3(expOf(parts[0]), expOf(parts[1]), expOf(parts[2]))));
        this.rows.add(row);
        return row;
    }

    VectorRow pair(String label, FloatMolangExp2 value, Consumer<FloatMolangExp2> commit) {
        VectorRow row = new VectorRow(this.screen, this.font, () -> this.fieldLabel(label), 2,
                new String[]{expText(value.exp1()), expText(value.exp2())},
                parts -> commit.accept(new FloatMolangExp2(expOf(parts[0]), expOf(parts[1]))));
        this.rows.add(row);
        return row;
    }

    /// 通用多行向量行：按 `values` 的行数各一行，提交时把 N 行文本原样交回（由调用方解析）。
    VectorRow vector(String label, int count, String[] values, Consumer<String[]> commit) {
        VectorRow row = new VectorRow(this.screen, this.font, () -> this.fieldLabel(label), count, values, commit);
        this.rows.add(row);
        return row;
    }

    /// 同上，但标签是动态的（例如按当前数据算出的 `gradient[0.5]`），不走字段翻译键。
    VectorRow vector(Supplier<String> label, int count, String[] values, Consumer<String[]> commit) {
        VectorRow row = new VectorRow(this.screen, this.font, label, count, values, commit);
        this.rows.add(row);
        return row;
    }

    /// 按钮行：点一下执行动作（用于「添加色标」这类结构性操作，动作里可以 `resetRows()` 重建行）。
    void button(String label, Runnable action) {
        ButtonRow row = new ButtonRow(this.screen, this.font, label, action);
        this.rows.add(row);
    }

    /// 按钮行（带行位置上界）：动作需要知道按钮所在 y 时用（例如在按钮下方展开事件下拉）。
    ButtonRow button(String label, IntConsumer action) {
        ButtonRow row = new ButtonRow(this.screen, this.font, label, action);
        this.rows.add(row);
        return row;
    }

    /// 直接加入一个自定义行（需要自己处理绘制与拖拽的行，例如调色盘 / 渐变色条）。
    void addRow(Row row) {
        this.rows.add(row);
    }

    SelectRow select(String label, List<String> options, int selected, IntConsumer commit) {
        SelectRow row = new SelectRow(this.screen, this.font, () -> this.fieldLabel(label), options, selected, commit);
        this.rows.add(row);
        return row;
    }

    /// 同上，标签由调用方动态给。
    SelectRow select(Supplier<String> label, List<String> options, int selected, IntConsumer commit) {
        SelectRow row = new SelectRow(this.screen, this.font, label, options, selected, commit);
        this.rows.add(row);
        return row;
    }

    ToggleRow toggle(String label, boolean value, Consumer<Boolean> commit) {
        ToggleRow row = new ToggleRow(this.screen, this.font, () -> this.fieldLabel(label), value, commit);
        this.rows.add(row);
        return row;
    }

    /// 同上，标签由调用方动态给。
    ToggleRow toggle(Supplier<String> label, boolean value, Consumer<Boolean> commit) {
        ToggleRow row = new ToggleRow(this.screen, this.font, label, value, commit);
        this.rows.add(row);
        return row;
    }

    InfoRow info(String label, String value) {
        InfoRow row = new InfoRow(this.screen, this.font, () -> this.fieldLabel(label), value);
        this.rows.add(row);
        return row;
    }

    /// 清掉全部行（子类型切换时由子类重建行布局）。
    protected void resetRows() {
        this.rows.clear();
    }

    // endregion

    int height() {
        int height = 0;
        for (Row row : this.rows) {
            height += row.height();
        }
        return height;
    }

    /// 行提交后打上标记；宿主消费后清掉。
    protected void markChanged() {
        this.changed = true;
    }

    boolean consumeChanged() {
        boolean value = this.changed;
        this.changed = false;
        return value;
    }

    void render(GuiGraphics guiGraphics, int top, int mouseX, int mouseY) {
        int y = top;
        for (Row row : this.rows) {
            row.render(guiGraphics, y, mouseX, mouseY);
            y += row.height();
        }
        // 展开的下拉列表画在所有行之上
        for (Row row : this.rows) {
            if (row instanceof SelectRow select && select.dropdownOpen()) {
                select.renderMenu(guiGraphics, mouseX, mouseY);
            }
        }
    }

    boolean mouseClicked(double mouseX, double mouseY) {
        for (Row row : this.rows) {
            if (row.mouseClicked(mouseX, mouseY)) {
                this.closeOtherRows(row);
                return true;
            }
        }
        this.closeEditing();
        return false;
    }

    private void closeOtherRows(Row target) {
        for (Row row : this.rows) {
            if (row != target) {
                row.closeEditing();
            }
        }
    }

    boolean mouseDragged(double mouseX, double mouseY) {
        for (Row row : this.rows) {
            if (row.mouseDragged(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    boolean mouseReleased(double mouseX, double mouseY) {
        boolean consumed = false;
        for (Row row : this.rows) {
            if (row.mouseReleased(mouseX, mouseY)) {
                consumed = true;
            }
        }
        return consumed;
    }

    boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (Row row : this.rows) {
            if (row.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    boolean charTyped(char codePoint, int modifiers) {
        for (Row row : this.rows) {
            if (row.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return false;
    }

    boolean isCapturingInput() {
        for (Row row : this.rows) {
            if (row.isCapturingInput()) {
                return true;
            }
        }
        return false;
    }

    boolean hasOpenMenus() {
        for (Row row : this.rows) {
            if (row instanceof SelectRow select && select.dropdownOpen()) {
                return true;
            }
        }
        return false;
    }

    void closeEditing() {
        for (Row row : this.rows) {
            row.closeEditing();
        }
    }

    boolean scroll(double mouseX, double mouseY, double scrollY) {
        for (Row row : this.rows) {
            if (row.scroll(mouseX, mouseY, scrollY)) {
                return true;
            }
        }
        return false;
    }

    boolean isOverOpenMenu(double mouseX, double mouseY) {
        for (Row row : this.rows) {
            if (row instanceof SelectRow select && select.dropdownOpen() && select.hitMenu(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    // region 值工具

    /// 空值占位文本（`particlestorm.editor.empty`），避免空字符串行看起来是空白。
    static String emptyText() {
        return Component.translatable("particle_editor.empty").getString();
    }

    static String expText(MolangExp exp) {
        if (exp instanceof FloatMolangExp floatExp && floatExp.getExpStr().isEmpty()) {
            return String.valueOf(floatExp.getConstant());
        }
        return exp.getExpStr();
    }

    static FloatMolangExp expOf(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return FloatMolangExp.ZERO;
        }
        try {
            return FloatMolangExp.ofConstant(Float.parseFloat(trimmed));
        } catch (NumberFormatException e) {
            return FloatMolangExp.ofExpression(trimmed);
        }
    }

    static MolangExp molangOf(String text) {
        String trimmed = text.trim();
        return trimmed.isEmpty() ? MolangExp.EMPTY : new MolangExp(trimmed);
    }

    static float floatOf(String text) {
        return Float.parseFloat(text.trim());
    }

    /// 下拉选项的显示名：`particlestorm.editor.minecraft.<group>.<value>`（没有翻译时原样显示值）。
    /// 同族子类型用组件路径做 value（如 `...minecraft.emitter_rate.emitter_rate_instant`）。
    protected static String optionLabel(String group, String value) {
        return Component.translatable("particle_editor.minecraft." + group + "." + value).getString();
    }

    /// 把一组原始选项值转成显示名（值本身不变，仍是下拉的下标语义）。
    protected static List<String> localizedOptions(String group, List<String> values) {
        return values.stream().map(value -> optionLabel(group, value)).toList();
    }

    /// 同族子类型 id → 本地化选项名（value 用组件 id 的完整 path，如 `emitter_rate_instant`）。
    protected static List<String> variantOptionLabels(List<ResourceLocation> ids, String group) {
        return ids.stream().map(id -> optionLabel(group, id.getPath())).toList();
    }

    // endregion

    /// 一行：标签 + 一个编辑控件。
    abstract static class Row {
        final Supplier<String> label;

        Row(Supplier<String> label) {
            this.label = label;
        }

        /// 行高（默认一行两行文本的高度；向量行按输入框数量变高）。
        int height() {
            return ROW_HEIGHT;
        }

        abstract void render(GuiGraphics guiGraphics, int top, int mouseX, int mouseY);

        /// 鼠标点击；返回 true 表示已消费。
        abstract boolean mouseClicked(double mouseX, double mouseY);

        /// 鼠标拖动（按住左键移动）；返回 true 表示已消费。
        boolean mouseDragged(double mouseX, double mouseY) {
            return false;
        }

        /// 鼠标松开；返回 true 表示曾处于拖动状态。
        boolean mouseReleased(double mouseX, double mouseY) {
            return false;
        }

        boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return false;
        }

        boolean charTyped(char codePoint, int modifiers) {
            return false;
        }

        boolean isCapturingInput() {
            return false;
        }

        void closeEditing() {
        }

        boolean scroll(double mouseX, double mouseY, double scrollY) {
            return false;
        }
    }

    static final class TextRow extends Row {
        private final EditorScreen screen;
        private final Font font;
        private final Consumer<String> commit;
        @Nullable
        private MolangEditBox box;
        private String value;
        private int top;

        TextRow(EditorScreen screen, Font font, Supplier<String> label, String value, Consumer<String> commit) {
            super(label);
            this.screen = screen;
            this.font = font;
            this.value = value;
            this.commit = commit;
        }

        void setValue(String value) {
            this.value = value;
        }

        @Override
        void render(GuiGraphics guiGraphics, int top, int mouseX, int mouseY) {
            this.top = top;
            int width = this.screen.toolboxWidth();
            if (this.box != null) {
                this.box.render(guiGraphics, mouseX, mouseY, 0);
                return;
            }
            boolean hover = mouseX >= 2 && mouseX < width - 4 && mouseY >= top && mouseY < top + ROW_HEIGHT;
            if (hover) {
                guiGraphics.fill(2, top, width - 4, top + ROW_HEIGHT, 0x20FFFFFF);
            }
            guiGraphics.drawString(this.font, this.label.get(), 6, top + LABEL_Y, 0x999999, false);
            int valueWidth = Math.max(10, width - 10 - VALUE_X);
            if (this.value.isEmpty()) {
                // 空值也要有可读文本，避免整行看起来是空白
                guiGraphics.drawString(this.font, emptyText(), VALUE_X, top + VALUE_Y, 0x777777, false);
                return;
            }
            if (hover && this.font.width(this.value) > valueWidth) {
                this.screen.highlightedTooltip = this.value;
            }
            MolangSyntax.draw(guiGraphics, this.font, this.value, VALUE_X, top + VALUE_Y, valueWidth);
        }

        @Override
        boolean mouseClicked(double mouseX, double mouseY) {
            if (this.box != null) {
                if (this.box.isMouseOver(mouseX, mouseY)) {
                    this.box.mouseClicked(mouseX, mouseY, 0);
                    return true;
                }
                return false;
            }
            int width = this.screen.toolboxWidth();
            if (mouseX < 2 || mouseX >= width - 4 || mouseY < this.top || mouseY >= this.top + ROW_HEIGHT) {
                return false;
            }
            this.open();
            return true;
        }

        private void open() {
            this.box = new MolangEditBox(this.font, 2, this.top + VALUE_Y - 2, Math.max(20, this.screen.toolboxWidth() - 4), 13, Component.literal(this.label.get()));
            this.box.setMaxLength(512);
            this.box.setValue(this.value);
            this.box.setFocused(true);
        }

        @Override
        boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (this.box == null) {
                return false;
            }
            if (keyCode == 256) { // ESC：取消
                this.box = null;
                return true;
            }
            if (keyCode == 257) { // ENTER：提交
                String text = this.box.getValue().trim();
                this.box = null;
                this.commit.accept(text);
                this.value = text;
                return true;
            }
            return this.box.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        boolean charTyped(char codePoint, int modifiers) {
            return this.box != null && this.box.charTyped(codePoint, modifiers);
        }

        @Override
        boolean mouseDragged(double mouseX, double mouseY) {
            // 编辑框里按住拖动 = 用鼠标选中文字
            return this.box != null && this.box.mouseDragged(mouseX, mouseY, 0, 0, 0);
        }

        @Override
        boolean mouseReleased(double mouseX, double mouseY) {
            return this.box != null && this.box.mouseReleased(mouseX, mouseY, 0);
        }

        @Override
        boolean isCapturingInput() {
            return this.box != null;
        }

        @Override
        void closeEditing() {
            this.box = null;
        }
    }

    /// 向量行：标签在上，下面按分量各占一行（FloatMolangExp2 两行、FloatMolangExp3 三行）。
    /// 与单行文本框一致：平时显示纯文本，点某一行才在该行打开 EditBox；`ENTER` 提交、`ESC` 取消、失焦自动提交。
    static final class VectorRow extends Row {
        private static final int LINE_HEIGHT = 14;
        private static final int FIRST_LINE_Y = VALUE_Y - 2;
        private static final int BOX_HEIGHT = 13;

        private final EditorScreen screen;
        private final Font font;
        private final int count;
        private final String[] values;
        private final Consumer<String[]> commit;
        @Nullable
        private MolangEditBox box;
        private int editingLine = -1;
        private int top;

        VectorRow(EditorScreen screen, Font font, Supplier<String> label, int count, String[] values, Consumer<String[]> commit) {
            super(label);
            this.screen = screen;
            this.font = font;
            this.count = count;
            this.values = values.clone();
            this.commit = commit;
        }

        @Override
        int height() {
            return ROW_HEIGHT + (this.count - 1) * LINE_HEIGHT;
        }

        void setValue(FloatMolangExp3 value) {
            if (this.count == 3) {
                this.values[0] = expText(value.exp1());
                this.values[1] = expText(value.exp2());
                this.values[2] = expText(value.exp3());
            }
        }

        @Override
        void render(GuiGraphics guiGraphics, int top, int mouseX, int mouseY) {
            this.top = top;
            int width = this.screen.toolboxWidth();
            guiGraphics.drawString(this.font, this.label.get(), 6, top + LABEL_Y, 0x999999, false);
            for (int i = 0; i < this.count; i++) {
                int lineY = top + FIRST_LINE_Y + i * LINE_HEIGHT;
                if (this.editingLine == i && this.box != null) {
                    this.box.setX(2);
                    this.box.setY(lineY);
                    this.box.setWidth(Math.max(20, width - 4));
                    this.box.render(guiGraphics, mouseX, mouseY, 0);
                    continue;
                }
                boolean hover = mouseX >= 2 && mouseX < width - 4 && mouseY >= lineY && mouseY < lineY + LINE_HEIGHT;
                if (hover) {
                    guiGraphics.fill(2, lineY, width - 4, lineY + LINE_HEIGHT, 0x20FFFFFF);
                }
                int valueWidth = Math.max(10, width - 10 - VALUE_X);
                if (this.values[i].isEmpty()) {
                    guiGraphics.drawString(this.font, emptyText(), VALUE_X, lineY + 3, 0x777777, false);
                    continue;
                }
                if (hover && this.font.width(this.values[i]) > valueWidth) {
                    this.screen.highlightedTooltip = this.values[i];
                }
                MolangSyntax.draw(guiGraphics, this.font, this.values[i], VALUE_X, lineY + 3, valueWidth);
            }
        }

        @Override
        boolean mouseClicked(double mouseX, double mouseY) {
            if (this.box != null) {
                if (this.box.isMouseOver(mouseX, mouseY)) {
                    this.box.mouseClicked(mouseX, mouseY, 0);
                    return true;
                }
                // 点别的行/外面：交给宿主 closeEditing()（提交当前行）
                return false;
            }
            int width = this.screen.toolboxWidth();
            if (mouseX < 2 || mouseX >= width - 4 || mouseY < this.top || mouseY >= this.top + this.height()) {
                return false;
            }
            int line = (int) ((mouseY - (this.top + FIRST_LINE_Y)) / LINE_HEIGHT);
            if (line < 0 || line >= this.count) {
                return false;
            }
            this.open(line);
            return true;
        }

        private void open(int line) {
            this.box = new MolangEditBox(this.font, 2, this.top + FIRST_LINE_Y + line * LINE_HEIGHT,
                    Math.max(20, this.screen.toolboxWidth() - 4), BOX_HEIGHT, Component.literal(this.label.get()));
            this.box.setMaxLength(512);
            this.box.setValue(this.values[line]);
            this.box.setFocused(true);
            this.editingLine = line;
        }

        @Override
        boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (this.box == null) {
                return false;
            }
            if (keyCode == 256) { // ESC：取消当前行编辑
                this.box = null;
                this.editingLine = -1;
                return true;
            }
            if (keyCode == 257) { // ENTER：提交当前行
                this.commitLine();
                return true;
            }
            return this.box.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        boolean charTyped(char codePoint, int modifiers) {
            return this.box != null && this.box.charTyped(codePoint, modifiers);
        }

        @Override
        boolean mouseDragged(double mouseX, double mouseY) {
            // 编辑框里按住拖动 = 用鼠标选中文字
            return this.box != null && this.box.mouseDragged(mouseX, mouseY, 0, 0, 0);
        }

        @Override
        boolean mouseReleased(double mouseX, double mouseY) {
            return this.box != null && this.box.mouseReleased(mouseX, mouseY, 0);
        }

        @Override
        boolean isCapturingInput() {
            return this.box != null;
        }

        @Override
        void closeEditing() {
            if (this.box != null) {
                this.commitLine();
            }
        }

        private void commitLine() {
            MolangEditBox current = this.box;
            int line = this.editingLine;
            this.box = null;
            this.editingLine = -1;
            if (current == null || line < 0) {
                return;
            }
            String value = current.getValue().trim();
            if (value.equals(this.values[line])) {
                return;
            }
            this.values[line] = value;
            this.commit.accept(this.values.clone());
        }
    }

    static final class SelectRow extends Row {
        private final EditorScreen screen;
        private final Font font;
        private final List<String> options;
        private final IntConsumer commit;
        private final DropdownWidget dropdown;
        private int selected;
        private int top;

        SelectRow(EditorScreen screen, Font font, Supplier<String> label, List<String> options, int selected, IntConsumer commit) {
            super(label);
            this.screen = screen;
            this.font = font;
            this.options = List.copyOf(options);
            this.selected = selected;
            this.commit = commit;
            this.dropdown = new DropdownWidget(2, 0, Math.max(20, screen.toolboxWidth() - 4), 12);
        }

        boolean dropdownOpen() {
            return this.dropdown.isOpen();
        }

        boolean hitMenu(double mouseX, double mouseY) {
            return this.dropdown.hitTest(mouseX, mouseY) >= 0;
        }

        @Override
        void render(GuiGraphics guiGraphics, int top, int mouseX, int mouseY) {
            this.top = top;
            int width = this.screen.toolboxWidth();
            boolean hover = mouseX >= 2 && mouseX < width - 4 && mouseY >= top && mouseY < top + ROW_HEIGHT;
            if (hover) {
                guiGraphics.fill(2, top, width - 4, top + ROW_HEIGHT, 0x20FFFFFF);
            }
            guiGraphics.drawString(this.font, this.label.get(), 6, top + LABEL_Y, 0x999999, false);
            String value = this.options.get(this.selected);
            guiGraphics.drawString(this.font, this.screen.truncatedFor(value, Math.max(10, width - 10 - VALUE_X), hover),
                    VALUE_X, top + VALUE_Y, 0xFFFFFF, false);
            this.screen.renderDropdownArrow(guiGraphics, width - 13, top + VALUE_Y + 2);
        }

        void renderMenu(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            this.dropdown.setItems(this.options);
            this.dropdown.setSelectedIndex(this.selected);
            this.dropdown.setPosition(2, this.top + ROW_HEIGHT);
            this.dropdown.render(guiGraphics, this.font, mouseX, mouseY, this.screen.height);
        }

        @Override
        boolean mouseClicked(double mouseX, double mouseY) {
            if (this.dropdown.isOpen()) {
                int index = this.dropdown.hitTest(mouseX, mouseY);
                this.dropdown.close();
                if (index >= 0) {
                    this.commit.accept(index);
                    this.selected = index;
                }
                return true;
            }
            int width = this.screen.toolboxWidth();
            if (mouseX < 2 || mouseX >= width - 4 || mouseY < this.top || mouseY >= this.top + ROW_HEIGHT) {
                return false;
            }
            this.dropdown.setPosition(2, this.top + ROW_HEIGHT);
            this.dropdown.toggle();
            return true;
        }

        @Override
        boolean isCapturingInput() {
            return this.dropdown.isOpen();
        }

        @Override
        void closeEditing() {
            this.dropdown.close();
        }

        @Override
        boolean scroll(double mouseX, double mouseY, double scrollY) {
            if (this.dropdown.contains(mouseX, mouseY)) {
                this.dropdown.scroll(scrollY);
                return true;
            }
            return false;
        }
    }

    static final class ButtonRow extends Row {
        private final EditorScreen screen;
        private final Font font;
        @Nullable
        private final Runnable action;
        @Nullable
        private final IntConsumer topAction;
        private int top;

        ButtonRow(EditorScreen screen, Font font, String label, Runnable action) {
            super(() -> label);
            this.screen = screen;
            this.font = font;
            this.action = action;
            this.topAction = null;
        }

        ButtonRow(EditorScreen screen, Font font, String label, IntConsumer topAction) {
            super(() -> label);
            this.screen = screen;
            this.font = font;
            this.action = null;
            this.topAction = topAction;
        }

        @Override
        void render(GuiGraphics guiGraphics, int top, int mouseX, int mouseY) {
            this.top = top;
            int width = this.screen.toolboxWidth();
            boolean hover = mouseX >= 2 && mouseX < width - 4 && mouseY >= top && mouseY < top + ROW_HEIGHT;
            if (hover) {
                guiGraphics.fill(2, top, width - 4, top + ROW_HEIGHT, 0x203A6FD8);
            }
            guiGraphics.drawString(this.font, this.screen.truncatedFor(this.label.get(), width - 12, hover),
                    6, top + LABEL_Y + 4, 0x9CD0FF, false);
        }

        @Override
        boolean mouseClicked(double mouseX, double mouseY) {
            int width = this.screen.toolboxWidth();
            if (mouseX < 2 || mouseX >= width - 4 || mouseY < this.top || mouseY >= this.top + ROW_HEIGHT) {
                return false;
            }
            if (this.topAction != null) {
                this.topAction.accept(this.top);
            } else if (this.action != null) {
                this.action.run();
            }
            return true;
        }
    }

    static final class ToggleRow extends Row {
        private final EditorScreen screen;
        private final Font font;
        private final Consumer<Boolean> commit;
        private boolean value;
        private int top;

        ToggleRow(EditorScreen screen, Font font, Supplier<String> label, boolean value, Consumer<Boolean> commit) {
            super(label);
            this.screen = screen;
            this.font = font;
            this.value = value;
            this.commit = commit;
        }

        void setValue(boolean value) {
            this.value = value;
        }

        @Override
        void render(GuiGraphics guiGraphics, int top, int mouseX, int mouseY) {
            this.top = top;
            int width = this.screen.toolboxWidth();
            boolean hover = mouseX >= 2 && mouseX < width - 4 && mouseY >= top && mouseY < top + ROW_HEIGHT;
            if (hover) {
                guiGraphics.fill(2, top, width - 4, top + ROW_HEIGHT, 0x20FFFFFF);
            }
            guiGraphics.drawString(this.font, this.label.get(), 6, top + LABEL_Y, 0x999999, false);
            guiGraphics.drawString(this.font, this.value ? "[x]" : "[ ]", VALUE_X, top + VALUE_Y, 0xFFFFFF, false);
        }

        @Override
        boolean mouseClicked(double mouseX, double mouseY) {
            int width = this.screen.toolboxWidth();
            if (mouseX < 2 || mouseX >= width - 4 || mouseY < this.top || mouseY >= this.top + ROW_HEIGHT) {
                return false;
            }
            this.value = !this.value;
            this.commit.accept(this.value);
            return true;
        }
    }

    static final class InfoRow extends Row {
        private final EditorScreen screen;
        private final Font font;
        private final String value;

        InfoRow(EditorScreen screen, Font font, Supplier<String> label, String value) {
            super(label);
            this.screen = screen;
            this.font = font;
            this.value = value;
        }

        @Override
        void render(GuiGraphics guiGraphics, int top, int mouseX, int mouseY) {
            int width = this.screen.toolboxWidth();
            boolean hover = mouseX >= 2 && mouseX < width - 4 && mouseY >= top && mouseY < top + ROW_HEIGHT;
            guiGraphics.drawString(this.font, this.label.get(), 6, top + LABEL_Y, 0x999999, false);
            guiGraphics.drawString(this.font, this.screen.truncatedFor(this.value, Math.max(10, width - 10 - VALUE_X), hover),
                    VALUE_X, top + VALUE_Y, 0xBBBBBB, false);
        }

        @Override
        boolean mouseClicked(double mouseX, double mouseY) {
            return false;
        }
    }
}

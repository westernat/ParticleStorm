package org.mesdag.particlestorm.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.mesdag.particlestorm.api.IComponent;
import org.mesdag.particlestorm.data.component.ParticleAppearanceTinting;
import org.mesdag.particlestorm.data.component.ParticleAppearanceTinting.Color;
import org.mesdag.particlestorm.data.component.ParticleAppearanceTinting.ColorField;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;

import java.util.*;

/// `particle_appearance_tinting` 的编辑 Grid。组件 codec 是 `Codec.either(Color.CODEC, ColorField.CODEC)`，
/// 对应三种编辑模式（「模式」下拉切换，两边数据各自保留）：
/// - **Static**（[ColorField] 常量）：一个调色盘（[ColorPicker]），编辑出来的永远是常量通道；
/// - **Gradient**（[Color]）：`interpolant` 表达式 + `range`（色标位置 × range = 渐变键，用来自动计算 gradient）
///   + 一条横向渐变条：[GradientRow] 里每个色标是一个可长按左右拖动的色块，旁边 `+` 添加色标（按最大空隙插入、
///   取插值出来的颜色、插入后自动选中），选中色标下方展开调色盘和 `×` 删除；
/// - **Expression**（[ColorField] 表达式）：红 / 绿 / 蓝 / 透明四个通道各一行 molang 表达式。
///
/// 渐变色的色标在编辑器里只有常量颜色（调色盘选出来的），所以载入时把色标里的表达式折叠成常量；
/// `build()` 只填当前模式那一边，另一边写 `EMPTY`（与 codec 写回时选 Either 的逻辑一致）。
final class ParticleAppearanceTintingGrid extends ComponentGrid {
    private static final int STATIC = 0;
    private static final int GRADIENT = 1;
    private static final int EXPRESSION = 2;
    private static final String DEFAULT_INTERPOLANT = "v.particle_age / v.particle_lifetime";
    private static final String[] DEFAULT_EXPR = {"1", "1", "1", "1"};
    private static final float[] DEFAULT_COLOR = {1.0F, 1.0F, 1.0F, 1.0F};

    private int mode = STATIC;
    private final ColorPicker staticPicker = new ColorPicker();
    private float[] staticColor = DEFAULT_COLOR.clone();
    private String interpolant = DEFAULT_INTERPOLANT;
    private String range = "1";
    /// 渐变色标：`[位置(0..1), 红, 绿, 蓝, 透明]`，按位置升序排列。
    private final List<float[]> stops = new ArrayList<>();
    private String[] expr = DEFAULT_EXPR.clone();

    ParticleAppearanceTintingGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.setComponentId(ParticleAppearanceTinting.ID);
        this.staticPicker.setColor(this.staticColor[0], this.staticColor[1], this.staticColor[2], this.staticColor[3]);
        this.addModeRow();
        this.addValueRows();
    }

    @Override
    public BuiltComponent createDefault() {
        return new BuiltComponent(ParticleAppearanceTinting.ID, new ParticleAppearanceTinting(Color.EMPTY, colorFieldOf(DEFAULT_COLOR)));
    }

    @Override
    public BuiltComponent build() {
        return switch (this.mode) {
            case GRADIENT -> new BuiltComponent(ParticleAppearanceTinting.ID, this.buildGradient());
            case EXPRESSION -> new BuiltComponent(ParticleAppearanceTinting.ID, new ParticleAppearanceTinting(Color.EMPTY,
                    colorFieldOf(this.expr[0], this.expr[1], this.expr[2], this.expr[3])));
            default -> new BuiltComponent(ParticleAppearanceTinting.ID, new ParticleAppearanceTinting(Color.EMPTY, colorFieldOf(this.staticColor)));
        };
    }

    @Override
    public void load(IComponent component) {
        ParticleAppearanceTinting tinting = (ParticleAppearanceTinting) component;
        ColorField field = tinting.colorField();
        if (field != ColorField.EMPTY) {
            if (isConstant(field.red()) && isConstant(field.green()) && isConstant(field.blue()) && isConstant(field.alpha())) {
                this.mode = STATIC;
                this.staticColor = new float[]{constantOf(field.red()), constantOf(field.green()), constantOf(field.blue()), constantOf(field.alpha())};
                this.staticPicker.setColor(this.staticColor[0], this.staticColor[1], this.staticColor[2], this.staticColor[3]);
                this.expr = DEFAULT_EXPR.clone();
            } else {
                this.mode = EXPRESSION;
                this.expr = new String[]{expText(field.red()), expText(field.green()), expText(field.blue()), expText(field.alpha())};
                this.staticColor = DEFAULT_COLOR.clone();
                this.staticPicker.setColor(this.staticColor[0], this.staticColor[1], this.staticColor[2], this.staticColor[3]);
            }
        } else {
            this.mode = GRADIENT;
            Color color = tinting.color();
            this.interpolant = expText(color.interpolant());
            float gradientRange = color.gradient().range;
            float scale = gradientRange > 0.0F ? gradientRange : 1.0F;
            this.range = Float.toString(scale);
            this.stops.clear();
            color.gradient().map.entrySet().stream()
                    .sorted(Comparator.comparingDouble(entry -> keyValue(entry.getKey())))
                    .forEach(entry -> {
                        ColorField stop = entry.getValue();
                        this.stops.add(new float[]{
                                Mth.clamp(keyValue(entry.getKey()) / scale, 0.0F, 1.0F),
                                constantOf(stop.red()), constantOf(stop.green()), constantOf(stop.blue()), constantOf(stop.alpha())
                        });
                    });
            if (this.stops.isEmpty()) {
                this.addDefaultStops();
            }
        }
        this.rebuildRows();
    }

    private ParticleAppearanceTinting buildGradient() {
        float rangeValue;
        try {
            rangeValue = Float.parseFloat(this.range.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid gradient range: " + this.range);
        }
        if (rangeValue <= 0.0F) {
            throw new IllegalArgumentException("Gradient range must be positive: " + this.range);
        }
        List<float[]> sorted = new ArrayList<>(this.stops);
        sorted.sort(Comparator.comparingDouble(stop -> stop[0]));
        Map<String, ColorField> map = new LinkedHashMap<>();
        for (float[] stop : sorted) {
            map.put(Float.toString(Mth.clamp(stop[0], 0.0F, 1.0F) * rangeValue),
                    colorFieldOf(new float[]{stop[1], stop[2], stop[3], stop[4]}));
        }
        return new ParticleAppearanceTinting(new Color(new Color.Gradient(map), expOf(this.interpolant)), ColorField.EMPTY);
    }

    private void addModeRow() {
        this.select("mode", List.of(
                localized("mode_static"),
                localized("mode_gradient"),
                localized("mode_expression")
        ), this.mode, index -> {
            this.mode = index;
            if (this.mode == GRADIENT && this.stops.isEmpty()) {
                this.addDefaultStops();
            }
            if (this.mode == STATIC) {
                this.staticPicker.setColor(this.staticColor[0], this.staticColor[1], this.staticColor[2], this.staticColor[3]);
            }
            this.rebuildRows();
            this.markChanged();
        });
    }

    private void addValueRows() {
        switch (this.mode) {
            case STATIC -> this.addRow(new PickerRow(this.screen, this.font, this.staticPicker, () -> {
                this.staticColor = new float[]{this.staticPicker.red(), this.staticPicker.green(), this.staticPicker.blue(), this.staticPicker.alpha()};
                this.markChanged();
            }));
            case GRADIENT -> {
                this.text("interpolant", this.interpolant, text -> {
                    this.interpolant = text;
                    this.markChanged();
                });
                this.text("range", this.range, text -> {
                    this.range = text;
                    this.markChanged();
                });
                this.addRow(new GradientRow(this.screen, this.font, this.stops, this::markChanged));
            }
            default -> this.vector("color", 4, this.expr, parts -> {
                this.expr = parts;
                this.markChanged();
            });
        }
    }

    private void rebuildRows() {
        this.resetRows();
        this.addModeRow();
        this.addValueRows();
    }

    private void addDefaultStops() {
        this.stops.clear();
        this.stops.add(new float[]{0.0F, 1.0F, 1.0F, 1.0F, 1.0F});
        this.stops.add(new float[]{1.0F, 1.0F, 1.0F, 1.0F, 1.0F});
    }

    private static String localized(String key) {
        return Component.translatable("particle_editor.minecraft.particle_appearance_tinting." + key).getString();
    }

    private static boolean isConstant(FloatMolangExp exp) {
        return exp.getExpStr().isEmpty();
    }

    private static float constantOf(FloatMolangExp exp) {
        return Mth.clamp(exp.getConstant(), 0.0F, 1.0F);
    }

    private static float keyValue(String text) {
        try {
            return Float.parseFloat(text.trim());
        } catch (NumberFormatException e) {
            return 0.0F;
        }
    }

    private static ColorField colorFieldOf(float[] color) {
        return new ColorField(FloatMolangExp.ofConstant(color[0]), FloatMolangExp.ofConstant(color[1]),
                FloatMolangExp.ofConstant(color[2]), FloatMolangExp.ofConstant(color[3]));
    }

    private static ColorField colorFieldOf(String red, String green, String blue, String alpha) {
        return new ColorField(expOf(red), expOf(green), expOf(blue), expOf(alpha));
    }
}

/// 静态调色盘行：把 [ColorPicker] 摆进 Grid，颜色变化时回调。
final class PickerRow extends ComponentGrid.Row {
    private final EditorScreen screen;
    private final Font font;
    private final ColorPicker picker;
    private final Runnable onChange;

    PickerRow(EditorScreen screen, Font font, ColorPicker picker, Runnable onChange) {
        super(() -> "");
        this.screen = screen;
        this.font = font;
        this.picker = picker;
        this.onChange = onChange;
    }

    @Override
    int height() {
        return this.picker.height() + 4;
    }

    @Override
    void render(GuiGraphics guiGraphics, int top, int mouseX, int mouseY) {
        this.picker.setBounds(2, top + 2, this.screen.toolboxWidth() - 4);
        this.picker.render(guiGraphics, this.font, mouseX, mouseY);
    }

    @Override
    boolean mouseClicked(double mouseX, double mouseY) {
        boolean pressed = this.picker.mousePressed(mouseX, mouseY);
        this.applyPickerChanged();
        return pressed;
    }

    @Override
    boolean mouseDragged(double mouseX, double mouseY) {
        boolean dragged = this.picker.mouseDragged(mouseX, mouseY);
        this.applyPickerChanged();
        return dragged;
    }

    @Override
    boolean mouseReleased(double mouseX, double mouseY) {
        boolean dragging = this.picker.isDragging();
        this.picker.mouseReleased();
        this.applyPickerChanged();
        return dragging;
    }

    @Override
    boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean handled = this.picker.keyPressed(keyCode);
        this.applyPickerChanged();
        return handled;
    }

    @Override
    boolean charTyped(char codePoint, int modifiers) {
        boolean handled = this.picker.charTyped(codePoint);
        this.applyPickerChanged();
        return handled;
    }

    @Override
    boolean isCapturingInput() {
        return this.picker.isCapturingInput();
    }

    @Override
    void closeEditing() {
        this.picker.commitEditing();
        this.applyPickerChanged();
    }

    private void applyPickerChanged() {
        if (this.picker.consumeChanged()) {
            this.onChange.run();
        }
    }
}

/// 渐变色条行：横向渐变预览 + 色块（长按左右拖动）+ `+` 添加色标；选中色标后下方展开调色盘与 `×` 删除。
final class GradientRow extends ComponentGrid.Row {
    private static final int BAR_HEIGHT = 18;
    private static final int BAR_TOP = 3;
    private static final int BLOCK_WIDTH = 7;
    private static final int PLUS_SIZE = 14;
    private static final int DELETE_SIZE = 12;
    private static final int PALETTE_GAP = 6;
    private static final int BOTTOM = 4;
    private static final float MIN_GAP = 0.001F;

    private final EditorScreen screen;
    private final Font font;
    /// 与 Grid 共享的色标列表：`[位置, 红, 绿, 蓝, 透明]`，按位置升序。
    private final List<float[]> stops;
    private final Runnable onChange;
    private final ColorPicker picker = new ColorPicker();
    private int selected = -1;
    private int dragIndex = -1;

    private int barX;
    private int barY;
    private int barWidth;
    private int plusX;
    private int plusY;
    private int paletteY;
    private int deleteX;

    GradientRow(EditorScreen screen, Font font, List<float[]> stops, Runnable onChange) {
        super(() -> "");
        this.screen = screen;
        this.font = font;
        this.stops = stops;
        this.onChange = onChange;
        if (!stops.isEmpty()) {
            this.select(0);
        }
    }

    @Override
    int height() {
        return BAR_TOP + BAR_HEIGHT + (this.selected >= 0 ? PALETTE_GAP + this.picker.height() : 0) + BOTTOM;
    }

    @Override
    void render(GuiGraphics guiGraphics, int top, int mouseX, int mouseY) {
        int width = this.screen.toolboxWidth();
        this.barX = 2;
        this.barY = top + BAR_TOP;
        this.plusX = width - 2 - PLUS_SIZE;
        this.plusY = this.barY + (BAR_HEIGHT - PLUS_SIZE) / 2;
        this.barWidth = Math.max(20, this.plusX - 3 - this.barX);

        // 渐变预览：一像素一列采样
        for (int i = 0; i < this.barWidth; i++) {
            float t = i / (float) Math.max(1, this.barWidth - 1);
            float[] color = this.sample(t);
            guiGraphics.fill(this.barX + i, this.barY, this.barX + i + 1, this.barY + BAR_HEIGHT, ColorPicker.argb(color[0], color[1], color[2], color[3]));
        }
        guiGraphics.renderOutline(this.barX - 1, this.barY - 1, this.barWidth + 2, BAR_HEIGHT + 2, 0xFF3A3A46);

        // 色块
        for (int i = 0; i < this.stops.size(); i++) {
            float[] stop = this.stops.get(i);
            int center = this.barX + Math.round(Mth.clamp(stop[0], 0.0F, 1.0F) * (this.barWidth - 1));
            int blockX = center - BLOCK_WIDTH / 2;
            int blockY = this.barY - 2;
            guiGraphics.fill(blockX, blockY, blockX + BLOCK_WIDTH, blockY + BAR_HEIGHT + 4,
                    ColorPicker.argb(stop[1], stop[2], stop[3], stop[4]));
            guiGraphics.renderOutline(blockX - 1, blockY - 1, BLOCK_WIDTH + 2, BAR_HEIGHT + 6, i == this.selected ? 0xFFFFFFFF : 0xFF222222);
        }

        // ＋
        boolean plusHover = mouseX >= this.plusX && mouseX < this.plusX + PLUS_SIZE && mouseY >= this.plusY && mouseY < this.plusY + PLUS_SIZE;
        guiGraphics.fill(this.plusX, this.plusY, this.plusX + PLUS_SIZE, this.plusY + PLUS_SIZE, plusHover ? 0xFF3A3A48 : 0xFF2A2A34);
        guiGraphics.renderOutline(this.plusX, this.plusY, PLUS_SIZE, PLUS_SIZE, 0xFF4A4A58);
        guiGraphics.drawString(this.font, "+", this.plusX + (PLUS_SIZE - this.font.width("+")) / 2, this.plusY + 2, 0xFFFFFF, false);

        // 选中色标的调色盘 + 删除
        if (this.selected >= 0 && this.selected < this.stops.size()) {
            this.paletteY = this.barY + BAR_HEIGHT + PALETTE_GAP;
            this.deleteX = width - 2 - DELETE_SIZE;
            int pickerWidth = Math.max(20, this.deleteX - 4);
            this.picker.setBounds(2, this.paletteY, pickerWidth);
            this.picker.render(guiGraphics, this.font, mouseX, mouseY);
            boolean deleteHover = mouseX >= this.deleteX && mouseX < this.deleteX + DELETE_SIZE
                    && mouseY >= this.paletteY && mouseY < this.paletteY + DELETE_SIZE;
            guiGraphics.fill(this.deleteX, this.paletteY, this.deleteX + DELETE_SIZE, this.paletteY + DELETE_SIZE,
                    deleteHover ? 0xA0FF4040 : 0x50FF4040);
            guiGraphics.renderOutline(this.deleteX, this.paletteY, DELETE_SIZE, DELETE_SIZE, 0xFF7A3030);
            guiGraphics.drawString(this.font, "x", this.deleteX + (DELETE_SIZE - this.font.width("x")) / 2, this.paletteY + 2, 0xFFFFFF, false);
        }
    }

    @Override
    boolean mouseClicked(double mouseX, double mouseY) {
        // 点任何地方都把正在编辑的读数格子先提交
        this.picker.commitEditing();
        this.applyPickerChanged();
        if (mouseX >= this.plusX && mouseX < this.plusX + PLUS_SIZE && mouseY >= this.plusY && mouseY < this.plusY + PLUS_SIZE) {
            this.addStop();
            return true;
        }
        if (this.selected >= 0 && mouseX >= this.deleteX && mouseX < this.deleteX + DELETE_SIZE
                && mouseY >= this.paletteY && mouseY < this.paletteY + DELETE_SIZE) {
            this.deleteSelected();
            return true;
        }
        if (this.selected >= 0 && this.picker.mousePressed(mouseX, mouseY)) {
            this.applyPickerChanged();
            return true;
        }
        if (mouseY >= this.barY - 3 && mouseY <= this.barY + BAR_HEIGHT + 3
                && mouseX >= this.barX - 5 && mouseX <= this.barX + this.barWidth + 5) {
            int hit = this.blockAt(mouseX);
            if (hit >= 0) {
                this.select(hit);
                this.dragIndex = hit;
            }
            return true;
        }
        return false;
    }

    @Override
    boolean mouseDragged(double mouseX, double mouseY) {
        if (this.dragIndex >= 0 && this.dragIndex < this.stops.size()) {
            float position = Mth.clamp((float) ((mouseX - this.barX) / Math.max(1, this.barWidth - 1)), 0.0F, 1.0F);
            float min = this.dragIndex > 0 ? this.stops.get(this.dragIndex - 1)[0] + MIN_GAP : 0.0F;
            float max = this.dragIndex < this.stops.size() - 1 ? this.stops.get(this.dragIndex + 1)[0] - MIN_GAP : 1.0F;
            float[] stop = this.stops.get(this.dragIndex);
            stop[0] = Mth.clamp(position, Math.min(min, max), Math.max(min, max));
            this.onChange.run();
            return true;
        }
        if (this.selected >= 0 && this.picker.mouseDragged(mouseX, mouseY)) {
            this.applyPickerChanged();
            return true;
        }
        return false;
    }

    @Override
    boolean mouseReleased(double mouseX, double mouseY) {
        boolean consumed = this.dragIndex >= 0 || this.picker.isDragging();
        this.dragIndex = -1;
        this.picker.mouseReleased();
        this.applyPickerChanged();
        return consumed;
    }

    @Override
    boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean handled = this.picker.keyPressed(keyCode);
        this.applyPickerChanged();
        return handled;
    }

    @Override
    boolean charTyped(char codePoint, int modifiers) {
        boolean handled = this.picker.charTyped(codePoint);
        this.applyPickerChanged();
        return handled;
    }

    @Override
    boolean isCapturingInput() {
        return this.picker.isCapturingInput();
    }

    @Override
    void closeEditing() {
        this.picker.commitEditing();
        this.applyPickerChanged();
    }

    private void applyPickerChanged() {
        if (this.picker.consumeChanged()) {
            this.applyPicker();
            this.onChange.run();
        }
    }

    private void select(int index) {
        if (index < 0 || index >= this.stops.size()) {
            return;
        }
        this.selected = index;
        float[] stop = this.stops.get(index);
        this.picker.setColor(stop[1], stop[2], stop[3], stop[4]);
    }

    private void applyPicker() {
        if (this.selected < 0 || this.selected >= this.stops.size()) {
            return;
        }
        float[] stop = this.stops.get(this.selected);
        stop[1] = this.picker.red();
        stop[2] = this.picker.green();
        stop[3] = this.picker.blue();
        stop[4] = this.picker.alpha();
    }

    private int blockAt(double mouseX) {
        int best = -1;
        int bestDistance = BLOCK_WIDTH / 2 + 3;
        for (int i = 0; i < this.stops.size(); i++) {
            float[] stop = this.stops.get(i);
            int center = this.barX + Math.round(Mth.clamp(stop[0], 0.0F, 1.0F) * (this.barWidth - 1));
            int distance = (int) Math.abs(mouseX - center);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    /// 添加色标：插到最大空隙中间，颜色取该位置的插值结果，插入后自动选中。
    private void addStop() {
        float position = this.biggestGapMidpoint();
        float[] color = this.sample(position);
        float[] stop = {position, color[0], color[1], color[2], color[3]};
        this.stops.add(stop);
        this.stops.sort(Comparator.comparingDouble(value -> value[0]));
        this.select(this.stops.indexOf(stop));
        this.onChange.run();
    }

    private void deleteSelected() {
        if (this.selected < 0 || this.stops.size() <= 1) {
            return;
        }
        this.stops.remove(this.selected);
        this.selected = Math.min(this.selected, this.stops.size() - 1);
        this.select(this.selected);
        this.onChange.run();
    }

    private float biggestGapMidpoint() {
        if (this.stops.isEmpty()) {
            return 0.5F;
        }
        float bestStart = 0.0F;
        float bestEnd = this.stops.getFirst()[0];
        for (int i = 1; i < this.stops.size(); i++) {
            float start = this.stops.get(i - 1)[0];
            float end = this.stops.get(i)[0];
            if (end - start > bestEnd - bestStart) {
                bestStart = start;
                bestEnd = end;
            }
        }
        float last = this.stops.getLast()[0];
        if (1.0F - last > bestEnd - bestStart) {
            bestStart = last;
            bestEnd = 1.0F;
        }
        return (bestStart + bestEnd) * 0.5F;
    }

    private float[] sample(float t) {
        if (this.stops.isEmpty()) {
            return new float[]{1.0F, 1.0F, 1.0F, 1.0F};
        }
        float[] previous = this.stops.getFirst();
        if (t <= previous[0]) {
            return new float[]{previous[1], previous[2], previous[3], previous[4]};
        }
        for (int i = 1; i < this.stops.size(); i++) {
            float[] next = this.stops.get(i);
            if (t <= next[0]) {
                float span = next[0] - previous[0];
                float factor = span <= 0.0F ? 0.0F : (t - previous[0]) / span;
                return new float[]{
                        previous[1] + (next[1] - previous[1]) * factor,
                        previous[2] + (next[2] - previous[2]) * factor,
                        previous[3] + (next[3] - previous[3]) * factor,
                        previous[4] + (next[4] - previous[4]) * factor
                };
            }
            previous = next;
        }
        return new float[]{previous[1], previous[2], previous[3], previous[4]};
    }
}

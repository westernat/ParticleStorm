package org.mesdag.particlestorm.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/// 调色盘：SV 方形色块（横轴饱和度、纵轴明度）+ 色相横条 + 不透明度横条，
/// 最底一行是 RGB / HSV / HEX 三选一的**可编辑**读数（点格子输入，`ENTER` / 点别处提交、`ESC` 取消），
/// 右侧上下两个箭头按钮（上下拼在一起）循环切换显示模式。
///
/// 颜色以 `[0,1]` 的 rgba 输出；坐标由使用方在每次绘制前用 [#setBounds] 更新。
final class ColorPicker {
    private static final int SQUARE_HEIGHT = 40;
    private static final int STRIP_HEIGHT = 8;
    private static final int GAP = 2;
    private static final int ROW_HEIGHT = 20;
    private static final int CELL_HEIGHT = 18;
    private static final int CELL_GAP = 2;
    private static final int ARROW_WIDTH = 12;
    private static final int ARROW_HEIGHT = 10;
    private static final int SQUARE = 0;
    private static final int HUE = 1;
    private static final int ALPHA = 2;
    private static final int DISPLAY_RGB = 0;
    private static final int DISPLAY_HSV = 1;
    private static final int DISPLAY_HEX = 2;
    private static final int DISPLAY_COUNT = 3;

    private int x;
    private int y;
    private int width = 40;
    private float hue = 1.0F;
    private float sat = 1.0F;
    private float val = 1.0F;
    private float alpha = 1.0F;
    private int drag = -1;
    private int displayMode = DISPLAY_RGB;
    private int editingIndex = -1;
    private String editText = "";
    private boolean changed;

    int height() {
        return SQUARE_HEIGHT + GAP + STRIP_HEIGHT + GAP + STRIP_HEIGHT + GAP + ROW_HEIGHT;
    }

    void setBounds(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = Math.max(20, width);
    }

    float red() {
        return rgb()[0];
    }

    float green() {
        return rgb()[1];
    }

    float blue() {
        return rgb()[2];
    }

    float alpha() {
        return this.alpha;
    }

    /// 从 rgba 反推 HSV（用于载入已有颜色）。
    void setColor(float red, float green, float blue, float alpha) {
        float r = Mth.clamp(red, 0.0F, 1.0F);
        float g = Mth.clamp(green, 0.0F, 1.0F);
        float b = Mth.clamp(blue, 0.0F, 1.0F);
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        this.val = max;
        this.sat = max <= 0.0F ? 0.0F : (max - min) / max;
        this.alpha = Mth.clamp(alpha, 0.0F, 1.0F);
        if (max <= min) {
            this.hue = 0.0F;
        } else if (max == r) {
            this.hue = ((g - b) / (max - min) / 6.0F + 1.0F) % 1.0F;
        } else if (max == g) {
            this.hue = (b - r) / (max - min) / 6.0F + 1.0F / 3.0F;
        } else {
            this.hue = (r - g) / (max - min) / 6.0F + 2.0F / 3.0F;
        }
    }

    void render(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY) {
        // SV 方形：横轴饱和度、纵轴明度（上明下暗）
        guiGraphics.fill(this.x - 1, this.y - 1, this.x + this.width + 1, this.y + SQUARE_HEIGHT + 1, 0xFF3A3A46);
        for (int i = 0; i < this.width; i++) {
            float s = this.width == 1 ? 0.0F : i / (float) (this.width - 1);
            float[] top = hsv(this.hue, s, 1.0F);
            float[] bottom = hsv(this.hue, s, 0.0F);
            guiGraphics.fillGradient(this.x + i, this.y, this.x + i + 1, this.y + SQUARE_HEIGHT,
                    argb(top[0], top[1], top[2], this.alpha), argb(bottom[0], bottom[1], bottom[2], this.alpha));
        }
        int markerX = this.x + Math.round(this.sat * (this.width - 1));
        int markerY = this.y + Math.round((1.0F - this.val) * (SQUARE_HEIGHT - 1));
        guiGraphics.fill(markerX - 2, markerY - 2, markerX + 3, markerY + 3, 0xFF000000);
        guiGraphics.fill(markerX - 1, markerY - 1, markerX + 2, markerY + 2, 0xFFFFFFFF);

        // 色相横条
        int hueY = this.y + SQUARE_HEIGHT + GAP;
        for (int i = 0; i < this.width; i++) {
            float t = this.width == 1 ? 0.0F : i / (float) (this.width - 1);
            float[] color = hsv(t, 1.0F, 1.0F);
            guiGraphics.fill(this.x + i, hueY, this.x + i + 1, hueY + STRIP_HEIGHT, argb(color[0], color[1], color[2], 1.0F));
        }
        drawIndicator(guiGraphics, hueY, this.hue);

        // 不透明度横条
        int alphaY = hueY + STRIP_HEIGHT + GAP;
        float[] current = rgb();
        for (int i = 0; i < this.width; i++) {
            float t = this.width == 1 ? 0.0F : i / (float) (this.width - 1);
            guiGraphics.fill(this.x + i, alphaY, this.x + i + 1, alphaY + STRIP_HEIGHT, argb(current[0], current[1], current[2], t));
        }
        drawIndicator(guiGraphics, alphaY, this.alpha);

        // 底行：可编辑读数格子 + 上下箭头
        int rowY = alphaY + STRIP_HEIGHT + GAP;
        int cellCount = this.cellCount();
        int cellWidth = this.cellWidth(cellCount);
        for (int i = 0; i < cellCount; i++) {
            int cellX = this.cellX(i, cellWidth);
            guiGraphics.fill(cellX, rowY, cellX + cellWidth, rowY + CELL_HEIGHT, i == this.editingIndex ? 0xFF3A3A55 : 0xFF22222C);
            guiGraphics.renderOutline(cellX, rowY, cellWidth, CELL_HEIGHT, 0xFF4A4A58);
            String text = i == this.editingIndex ? this.editText + "_" : this.cellText(i);
            guiGraphics.drawString(font, font.plainSubstrByWidth(text, cellWidth - 4), cellX + 2, rowY + 5, 0xFFFFFF, false);
        }
        int arrowX = this.x + this.width - ARROW_WIDTH;
        this.drawArrow(guiGraphics, font, arrowX, rowY, "^", mouseX, mouseY);
        this.drawArrow(guiGraphics, font, arrowX, rowY + ARROW_HEIGHT, "v", mouseX, mouseY);
    }

    private void drawIndicator(GuiGraphics guiGraphics, int stripY, float value) {
        int indicator = this.x + Math.round(value * (this.width - 1));
        guiGraphics.fill(indicator, stripY - 1, indicator + 1, stripY + STRIP_HEIGHT + 1, 0xFFFFFFFF);
    }

    private void drawArrow(GuiGraphics guiGraphics, Font font, int arrowX, int arrowY, String glyph, int mouseX, int mouseY) {
        boolean hover = mouseX >= arrowX && mouseX < arrowX + ARROW_WIDTH && mouseY >= arrowY && mouseY < arrowY + ARROW_HEIGHT;
        guiGraphics.fill(arrowX, arrowY, arrowX + ARROW_WIDTH, arrowY + ARROW_HEIGHT, hover ? 0xFF3A3A48 : 0xFF2A2A34);
        guiGraphics.renderOutline(arrowX, arrowY, ARROW_WIDTH, ARROW_HEIGHT, 0xFF4A4A58);
        guiGraphics.drawString(font, glyph, arrowX + (ARROW_WIDTH - font.width(glyph)) / 2, arrowY + 1, 0xFFFFFF, false);
    }

    private int rowY() {
        return this.y + SQUARE_HEIGHT + GAP + STRIP_HEIGHT + GAP + STRIP_HEIGHT + GAP;
    }

    private int cellCount() {
        return this.displayMode == DISPLAY_HEX ? 1 : 4;
    }

    private int cellWidth(int cellCount) {
        int available = this.width - 2 - ARROW_WIDTH - 2;
        return Math.max(8, (available - CELL_GAP * (cellCount - 1)) / cellCount);
    }

    private int cellX(int index, int cellWidth) {
        return this.x + index * (cellWidth + CELL_GAP);
    }

    private String cellText(int index) {
        float[] color = rgb();
        return switch (this.displayMode) {
            case DISPLAY_HSV -> switch (index) {
                case 0 -> "H:" + Math.round(this.hue * 360.0F);
                case 1 -> "S:" + Math.round(this.sat * 100.0F);
                case 2 -> "V:" + Math.round(this.val * 100.0F);
                default -> "A:" + Math.round(this.alpha * 100.0F);
            };
            case DISPLAY_HEX -> this.hexText();
            default -> switch (index) {
                case 0 -> "R:" + Math.round(color[0] * 255.0F);
                case 1 -> "G:" + Math.round(color[1] * 255.0F);
                case 2 -> "B:" + Math.round(color[2] * 255.0F);
                default -> "A:" + Math.round(this.alpha * 255.0F);
            };
        };
    }

    private String editText(int index) {
        float[] color = rgb();
        return switch (this.displayMode) {
            case DISPLAY_HSV -> switch (index) {
                case 0 -> String.valueOf(Math.round(this.hue * 360.0F));
                case 1 -> String.valueOf(Math.round(this.sat * 100.0F));
                case 2 -> String.valueOf(Math.round(this.val * 100.0F));
                default -> String.valueOf(Math.round(this.alpha * 100.0F));
            };
            case DISPLAY_HEX -> this.hexText();
            default -> switch (index) {
                case 0 -> String.valueOf(Math.round(color[0] * 255.0F));
                case 1 -> String.valueOf(Math.round(color[1] * 255.0F));
                case 2 -> String.valueOf(Math.round(color[2] * 255.0F));
                default -> String.valueOf(Math.round(this.alpha * 255.0F));
            };
        };
    }

    private String hexText() {
        float[] color = rgb();
        int a = Math.round(this.alpha * 255.0F);
        int r = Math.round(color[0] * 255.0F);
        int g = Math.round(color[1] * 255.0F);
        int b = Math.round(color[2] * 255.0F);
        return a >= 255 ? String.format("#%02X%02X%02X", r, g, b) : String.format("#%02X%02X%02X%02X", r, g, b, a);
    }

    /// 点在方格 / 色相条 / 不透明度条上时开始拖动，点读数格子进入编辑，点上下箭头切换显示模式。
    boolean mousePressed(double mouseX, double mouseY) {
        this.commitEditing();
        int rowY = this.rowY();
        if (mouseY >= rowY && mouseY < rowY + CELL_HEIGHT) {
            int arrowX = this.x + this.width - ARROW_WIDTH;
            if (mouseX >= arrowX && mouseX < arrowX + ARROW_WIDTH) {
                if (mouseY < rowY + ARROW_HEIGHT) {
                    this.displayMode = (this.displayMode + DISPLAY_COUNT - 1) % DISPLAY_COUNT;
                    return true;
                }
                this.displayMode = (this.displayMode + 1) % DISPLAY_COUNT;
                return true;
            }
            int cellCount = this.cellCount();
            int cellWidth = this.cellWidth(cellCount);
            for (int i = 0; i < cellCount; i++) {
                int cellX = this.cellX(i, cellWidth);
                if (mouseX >= cellX && mouseX < cellX + cellWidth) {
                    this.beginEditing(i);
                    return true;
                }
            }
            return false;
        }
        int hit = this.hitPart(mouseY);
        if (hit < 0 || mouseX < this.x - 2 || mouseX > this.x + this.width + 2) {
            return false;
        }
        this.drag = hit;
        this.update(hit, mouseX, mouseY);
        return true;
    }

    boolean mouseDragged(double mouseX, double mouseY) {
        if (this.drag < 0) {
            return false;
        }
        this.update(this.drag, mouseX, mouseY);
        return true;
    }

    void mouseReleased() {
        this.drag = -1;
    }

    boolean isDragging() {
        return this.drag >= 0;
    }

    boolean isCapturingInput() {
        return this.editingIndex >= 0;
    }

    /// 提交当前格子的输入；返回是否真的改了颜色。
    void commitEditing() {
        if (this.editingIndex < 0) {
            return;
        }
        int index = this.editingIndex;
        String text = this.editText.trim();
        this.editingIndex = -1;
        this.editText = "";
        boolean applied = switch (this.displayMode) {
            case DISPLAY_HSV -> this.applyHsvCell(index, text);
            case DISPLAY_HEX -> this.applyHex(text);
            default -> this.applyRgbCell(index, text);
        };
        if (applied) {
            this.changed = true;
        }
    }

    boolean keyPressed(int keyCode) {
        if (this.editingIndex < 0) {
            return false;
        }
        switch (keyCode) {
            case 256 -> { // ESC：取消
                this.editingIndex = -1;
                this.editText = "";
            }
            case 257, 258 -> this.commitEditing(); // ENTER / TAB
            case 259 -> this.editText = this.editText.isEmpty() ? "" : this.editText.substring(0, this.editText.length() - 1);
            default -> {
            }
        }
        return true;
    }

    boolean charTyped(char codePoint) {
        if (this.editingIndex < 0) {
            return false;
        }
        boolean allowed = this.displayMode == DISPLAY_HEX
                ? isHexChar(codePoint)
                : Character.isDigit(codePoint);
        if (allowed) {
            this.editText += codePoint;
        }
        return true;
    }

    /// 交互是否改动了颜色（由行宿主取走后重建组件）。
    boolean consumeChanged() {
        boolean value = this.changed;
        this.changed = false;
        return value;
    }

    private void beginEditing(int index) {
        this.editingIndex = index;
        this.editText = this.editText(index);
    }

    private boolean applyRgbCell(int index, String text) {
        float value = parseByte(text);
        if (value < 0.0F) {
            return false;
        }
        float[] color = rgb();
        float red = color[0];
        float green = color[1];
        float blue = color[2];
        switch (index) {
            case 0 -> red = value;
            case 1 -> green = value;
            case 2 -> blue = value;
            default -> this.alpha = value;
        }
        this.setColor(red, green, blue, this.alpha);
        return true;
    }

    private boolean applyHsvCell(int index, String text) {
        try {
            float value = Float.parseFloat(text);
            switch (index) {
                case 0 -> this.hue = Mth.clamp(value / 360.0F, 0.0F, 1.0F) % 1.0F;
                case 1 -> this.sat = Mth.clamp(value / 100.0F, 0.0F, 1.0F);
                case 2 -> this.val = Mth.clamp(value / 100.0F, 0.0F, 1.0F);
                default -> this.alpha = Mth.clamp(value / 100.0F, 0.0F, 1.0F);
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean applyHex(String text) {
        String hex = text.startsWith("#") ? text.substring(1) : text;
        if (hex.length() != 6 && hex.length() != 8) {
            return false;
        }
        try {
            float a = hex.length() == 8 ? Integer.parseInt(hex.substring(6, 8), 16) / 255.0F : 1.0F;
            float r = Integer.parseInt(hex.substring(0, 2), 16) / 255.0F;
            float g = Integer.parseInt(hex.substring(2, 4), 16) / 255.0F;
            float b = Integer.parseInt(hex.substring(4, 6), 16) / 255.0F;
            this.setColor(r, g, b, a);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static float parseByte(String text) {
        if (text.isEmpty()) {
            return -1.0F;
        }
        try {
            int value = Integer.parseInt(text);
            return Mth.clamp(value, 0, 255) / 255.0F;
        } catch (NumberFormatException e) {
            return -1.0F;
        }
    }

    private static boolean isHexChar(char c) {
        return Character.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F') || c == '#';
    }

    private int hitPart(double mouseY) {
        if (mouseY >= this.y - 1 && mouseY < this.y + SQUARE_HEIGHT + 1) {
            return SQUARE;
        }
        int hueY = this.y + SQUARE_HEIGHT + GAP;
        if (mouseY >= hueY - 1 && mouseY < hueY + STRIP_HEIGHT + 1) {
            return HUE;
        }
        int alphaY = hueY + STRIP_HEIGHT + GAP;
        if (mouseY >= alphaY - 1 && mouseY < alphaY + STRIP_HEIGHT + 1) {
            return ALPHA;
        }
        return -1;
    }

    private void update(int part, double mouseX, double mouseY) {
        float tx = Mth.clamp((float) ((mouseX - this.x) / Math.max(1, this.width - 1)), 0.0F, 1.0F);
        switch (part) {
            case SQUARE -> {
                this.sat = tx;
                this.val = 1.0F - Mth.clamp((float) ((mouseY - this.y) / Math.max(1, SQUARE_HEIGHT - 1)), 0.0F, 1.0F);
            }
            case HUE -> this.hue = tx;
            default -> this.alpha = tx;
        }
        this.changed = true;
    }

    private float[] rgb() {
        return hsv(this.hue, this.sat, this.val);
    }

    private static float[] hsv(float hue, float saturation, float value) {
        float h = (hue % 1.0F + 1.0F) % 1.0F * 6.0F;
        int i = (int) Math.floor(h);
        float f = h - i;
        float p = value * (1.0F - saturation);
        float q = value * (1.0F - saturation * f);
        float t = value * (1.0F - saturation * (1.0F - f));
        return switch (i % 6) {
            case 0 -> new float[]{value, t, p};
            case 1 -> new float[]{q, value, p};
            case 2 -> new float[]{p, value, t};
            case 3 -> new float[]{p, q, value};
            case 4 -> new float[]{t, p, value};
            default -> new float[]{value, p, q};
        };
    }

    static int argb(float red, float green, float blue, float alpha) {
        int a = Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        int r = Math.round(Mth.clamp(red, 0.0F, 1.0F) * 255.0F);
        int g = Math.round(Mth.clamp(green, 0.0F, 1.0F) * 255.0F);
        int b = Math.round(Mth.clamp(blue, 0.0F, 1.0F) * 255.0F);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}

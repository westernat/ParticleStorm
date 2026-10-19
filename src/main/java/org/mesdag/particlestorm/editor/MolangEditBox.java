package org.mesdag.particlestorm.editor;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/// 自带 Molang 语法高亮的单行编辑框（完全自绘，不继承原版 EditBox）。
///
/// 支持：输入、退格/Delete、左右移动（Ctrl 按词）、Home/End、点击定位、**鼠标拖动选中**、
/// **Shift+方向键选中**、**Ctrl+A 全选**、**Ctrl+C/X/V 复制/剪切/粘贴**、`ENTER`/`ESC` 由宿主行处理；
/// 文本超出内宽时自己维护显示起点；选中部分是半透明蓝底，光标在末尾画 `_`、中间画竖线并 300ms 闪烁。
final class MolangEditBox extends AbstractWidget {
    private static final int CURSOR_BLINK_MS = 300;
    private static final int PADDING = 4;
    private static final int CURSOR_COLOR = 0xFFD0D0D0;
    private static final int DEFAULT_TEXT_COLOR = 0xFFE0E0E0;
    private static final int SELECTION_COLOR = 0x8030A0FF;

    private final Font font;
    private String value = "";
    private int maxLength = 512;
    private int cursorPos;
    /// 选区的另一端：与 `cursorPos` 不同即表示有选区。
    private int highlightPos;
    private int displayPos;
    private long focusedTime;
    private boolean selecting;

    MolangEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
        this.font = font;
    }

    void setMaxLength(int maxLength) {
        this.maxLength = Math.max(1, maxLength);
        if (this.value.length() > this.maxLength) {
            this.value = this.value.substring(0, this.maxLength);
            this.cursorPos = Math.min(this.cursorPos, this.value.length());
            this.highlightPos = Math.min(this.highlightPos, this.value.length());
        }
    }

    void setValue(String value) {
        this.value = value.length() > this.maxLength ? value.substring(0, this.maxLength) : value;
        this.cursorPos = this.value.length();
        this.highlightPos = this.cursorPos;
        this.displayPos = 0;
    }

    String getValue() {
        return this.value;
    }

    int getInnerWidth() {
        return Math.max(1, this.getWidth() - PADDING * 2);
    }

    @Override
    public void setFocused(boolean focused) {
        if (focused && !this.isFocused()) {
            this.focusedTime = Util.getMillis();
        }
        super.setFocused(focused);
    }

    // region 渲染

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = this.getX();
        int y = this.getY();
        int textY = y + (this.getHeight() - 8) / 2;
        guiGraphics.fill(x - 1, y - 1, x + this.getWidth() + 1, y + this.getHeight() + 1, 0xFFA0A0A0);
        guiGraphics.fill(x, y, x + this.getWidth(), y + this.getHeight(), 0xFF000000);
        this.updateDisplayPos();
        String visible = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
        this.drawSelection(guiGraphics, x + PADDING, textY, visible);
        // 按整段文本分词（从 displayPos 开始画）：横向滚动截掉 `v.` 前缀时高亮也不会丢
        MolangSyntax.drawRange(guiGraphics, this.font, this.value, this.displayPos, x + PADDING, textY, this.getInnerWidth());
        int cursor = Math.min(this.cursorPos, this.value.length());
        if (cursor >= this.displayPos && cursor <= this.displayPos + visible.length()
                && (Util.getMillis() - this.focusedTime) / CURSOR_BLINK_MS % 2 == 0) {
            int cursorX = x + PADDING + this.font.width(this.value.substring(this.displayPos, cursor));
            if (cursor >= this.value.length()) {
                guiGraphics.drawString(this.font, "_", cursorX, textY, DEFAULT_TEXT_COLOR, false);
            } else {
                guiGraphics.fill(cursorX, textY - 1, cursorX + 1, textY + 9, CURSOR_COLOR);
            }
        }
    }

    private void drawSelection(GuiGraphics guiGraphics, int textX, int textY, String visible) {
        int from = Math.min(this.cursorPos, this.highlightPos);
        int to = Math.max(this.cursorPos, this.highlightPos);
        if (from == to) {
            return;
        }
        from = Math.max(from, this.displayPos);
        to = Math.min(to, this.displayPos + visible.length());
        if (from >= to) {
            return;
        }
        int startX = textX + this.font.width(this.value.substring(this.displayPos, from));
        int endX = startX + this.font.width(this.value.substring(from, to));
        guiGraphics.fill(startX, textY - 1, endX, textY + 9, SELECTION_COLOR);
    }

    /// 显示起点跟随光标：光标在起点左边就回退，光标所在地前缀超宽就右移。
    private void updateDisplayPos() {
        this.displayPos = Math.min(this.displayPos, this.value.length());
        if (this.displayPos > this.cursorPos) {
            this.displayPos = this.cursorPos;
        }
        while (this.displayPos < this.cursorPos
                && this.font.width(this.value.substring(this.displayPos, this.cursorPos)) > this.getInnerWidth()) {
            this.displayPos++;
        }
    }

    // endregion

    // region 鼠标

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || button != 0 || !this.clicked(mouseX, mouseY)) {
            return false;
        }
        this.cursorPos = this.indexAt(mouseX);
        this.highlightPos = this.cursorPos;
        this.selecting = true;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.selecting) {
            return false;
        }
        this.cursorPos = this.indexAt(mouseX);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean wasSelecting = this.selecting;
        this.selecting = false;
        return wasSelecting;
    }

    private int indexAt(double mouseX) {
        String visible = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
        String before = this.font.plainSubstrByWidth(visible, (int) Math.max(0, mouseX - this.getX() - PADDING));
        return Math.min(this.value.length(), this.displayPos + before.length());
    }

    // endregion

    // region 键盘

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.isFocused()) {
            return false;
        }
        boolean shift = Screen.hasShiftDown();
        boolean control = Screen.hasControlDown();
        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (this.hasSelection()) {
                    this.deleteSelection();
                } else if (this.cursorPos > 0) {
                    this.deleteRange(this.cursorPos - 1, this.cursorPos);
                }
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (this.hasSelection()) {
                    this.deleteSelection();
                } else if (this.cursorPos < this.value.length()) {
                    this.deleteRange(this.cursorPos, this.cursorPos + 1);
                }
            }
            case GLFW.GLFW_KEY_LEFT -> this.moveCursor(-1, shift, control);
            case GLFW.GLFW_KEY_RIGHT -> this.moveCursor(1, shift, control);
            case GLFW.GLFW_KEY_HOME -> this.setCursor(0, shift);
            case GLFW.GLFW_KEY_END -> this.setCursor(this.value.length(), shift);
            default -> {
                if (!control) {
                    return false;
                }
                switch (keyCode) {
                    case GLFW.GLFW_KEY_A -> this.selectAll();
                    case GLFW.GLFW_KEY_C -> this.copySelection();
                    case GLFW.GLFW_KEY_X -> {
                        this.copySelection();
                        this.deleteSelection();
                    }
                    case GLFW.GLFW_KEY_V -> this.paste();
                    default -> {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!this.isFocused() || codePoint < ' ') {
            return false;
        }
        this.replaceSelection(String.valueOf(codePoint));
        return true;
    }

    private boolean hasSelection() {
        return this.cursorPos != this.highlightPos;
    }

    private void selectAll() {
        this.highlightPos = 0;
        this.cursorPos = this.value.length();
    }

    private void setCursor(int position, boolean extend) {
        int clamped = Math.max(0, Math.min(this.value.length(), position));
        this.cursorPos = clamped;
        if (!extend) {
            this.highlightPos = clamped;
        }
    }

    private void moveCursor(int direction, boolean extend, boolean byWord) {
        int length = this.value.length();
        int next = this.cursorPos;
        if (byWord) {
            if (direction < 0) {
                while (next > 0 && Character.isWhitespace(this.value.charAt(next - 1))) {
                    next--;
                }
                while (next > 0 && !Character.isWhitespace(this.value.charAt(next - 1))) {
                    next--;
                }
            } else {
                while (next < length && !Character.isWhitespace(this.value.charAt(next))) {
                    next++;
                }
                while (next < length && Character.isWhitespace(this.value.charAt(next))) {
                    next++;
                }
            }
        } else {
            next = Math.max(0, Math.min(length, next + direction));
        }
        this.setCursor(next, extend);
    }

    private void deleteRange(int from, int to) {
        int start = Math.max(0, Math.min(from, this.value.length()));
        int end = Math.max(start, Math.min(to, this.value.length()));
        this.value = this.value.substring(0, start) + this.value.substring(end);
        this.cursorPos = start;
        this.highlightPos = start;
    }

    private void deleteSelection() {
        this.deleteRange(Math.min(this.cursorPos, this.highlightPos), Math.max(this.cursorPos, this.highlightPos));
    }

    private void replaceSelection(String text) {
        if (text.isEmpty()) {
            return;
        }
        if (this.hasSelection()) {
            this.deleteSelection();
        }
        int room = this.maxLength - this.value.length();
        if (room <= 0) {
            return;
        }
        String inserted = text.length() > room ? text.substring(0, room) : text;
        this.value = this.value.substring(0, this.cursorPos) + inserted + this.value.substring(this.cursorPos);
        this.cursorPos += inserted.length();
        this.highlightPos = this.cursorPos;
    }

    private void copySelection() {
        if (!this.hasSelection()) {
            return;
        }
        int from = Math.min(this.cursorPos, this.highlightPos);
        int to = Math.max(this.cursorPos, this.highlightPos);
        Minecraft.getInstance().keyboardHandler.setClipboard(this.value.substring(from, to));
    }

    private void paste() {
        String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clipboard.isEmpty()) {
            return;
        }
        // 单行输入：把换行折成空格
        this.replaceSelection(clipboard.replace('\r', ' ').replace('\n', ' '));
    }

    // endregion

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
    }
}

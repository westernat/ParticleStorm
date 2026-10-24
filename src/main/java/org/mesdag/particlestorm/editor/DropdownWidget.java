package org.mesdag.particlestorm.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/// 编辑器的简单下拉列表组件：只管「列表」的渲染、滚动与命中检测，按钮本身由调用方绘制。
///
/// 调用方维护打开状态（[#toggle] / [#close]），每帧调用 [#render]；
/// 点击时用 [#hitTest] 取条目下标，滚轮用 [#contains] + [#scroll] 判断并滚动，
/// 截断文本的 tooltip 由调用方根据 [#hitTest] / [#item] / [#textWidth] 决定。
///
/// 列表宽度不跟按钮走：每帧按「当前条目里最长的文本宽度 + 左右内边距」算，`minWidth` 只是下限。
final class DropdownWidget {
    /// 列表左右内边距（左侧 5、右侧 5）。
    private static final int PADDING = 10;

    private int x;
    private int listTop;
    private final int minWidth;
    private final int rowHeight;
    private int renderWidth;
    private List<String> items = List.of();
    private int selectedIndex = -1;
    private boolean open;
    private int visibleCapacity;
    private int renderedRows;
    private int scrollOffset;

    public DropdownWidget(int x, int listTop, int minWidth, int rowHeight) {
        this.x = x;
        this.listTop = listTop;
        this.minWidth = minWidth;
        this.renderWidth = minWidth;
        this.rowHeight = rowHeight;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    /// 重新摆放列表（工具框宽度随窗口大小变化时用）。
    public void setPosition(int x, int listTop) {
        this.x = x;
        this.listTop = listTop;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }

    public @Nullable String item(int index) {
        return index >= 0 && index < items.size() ? items.get(index) : null;
    }

    /// 当前列表实际宽度（按最长条目算出来的，渲染后才有意义）。
    public int textWidth() {
        return Math.max(1, this.renderWidth - PADDING);
    }

    public boolean isOpen() {
        return this.open;
    }

    public void toggle() {
        this.open = !this.open;
        if (this.open) {
            this.scrollOffset = 0;
            if (this.renderedRows == 0) {
                // 打开后、下一次 render 之前，hitTest 也要能命中（按全部条目算，render 时会再按可见高度截断）
                this.renderedRows = this.items.size();
            }
        }
    }

    public void close() {
        this.open = false;
    }

    /// 在当前鼠标位置绘制列表；行数按 `maxBottom` 截断（超出屏幕的部分不画、也不可点），超出部分用滚动访问。
    public void render(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY, int maxBottom) {
        if (!this.open) {
            this.renderedRows = 0;
            return;
        }
        // 宽度 = 最长条目文本 + 内边距，至少 minWidth（按钮宽度）
        int longest = 0;
        for (String item : this.items) {
            longest = Math.max(longest, font.width(item));
        }
        this.renderWidth = Math.max(this.minWidth, longest + PADDING);
        this.visibleCapacity = Math.max(0, (maxBottom - this.listTop) / this.rowHeight);
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, Math.max(0, this.items.size() - this.visibleCapacity));
        this.renderedRows = Math.min(this.items.size() - this.scrollOffset, this.visibleCapacity);
        if (this.renderedRows <= 0) {
            return;
        }
        int listBottom = this.listTop + this.renderedRows * this.rowHeight;
        guiGraphics.fill(this.x, this.listTop, this.x + this.renderWidth, listBottom, 0xF0181820);
        guiGraphics.renderOutline(this.x, this.listTop, this.renderWidth, this.renderedRows * this.rowHeight, 0xFF4A4A58);
        for (int i = 0; i < this.renderedRows; i++) {
            int rowTop = this.listTop + i * this.rowHeight;
            int itemIndex = this.scrollOffset + i;
            boolean hover = mouseX >= this.x && mouseX < this.x + this.renderWidth && mouseY >= rowTop && mouseY < rowTop + this.rowHeight;
            boolean selected = itemIndex == this.selectedIndex;
            if (hover) {
                guiGraphics.fill(this.x + 1, rowTop, this.x + this.renderWidth - 1, rowTop + this.rowHeight, 0x40FFFFFF);
            } else if (selected) {
                guiGraphics.fill(this.x + 1, rowTop, this.x + this.renderWidth - 1, rowTop + this.rowHeight, 0x30226699);
            }
            String text = this.items.get(itemIndex);
            int textWidth = this.textWidth();
            if (font.width(text) > textWidth) {
                text = font.plainSubstrByWidth(text, textWidth - font.width("...")) + "...";
            }
            guiGraphics.drawString(font, text, this.x + 5, rowTop + 2, hover || selected ? 0xFFFFFF : 0xBBBBBB, false);
        }
        this.renderScrollbar(guiGraphics, listBottom);
    }

    /// 列表放不下时，右缘画一条滚动条（轨道 + 滑块）提示可滚动。
    private void renderScrollbar(GuiGraphics guiGraphics, int listBottom) {
        if (this.items.size() <= this.renderedRows) {
            return;
        }
        int trackHeight = listBottom - this.listTop;
        int thumbHeight = Math.max(4, trackHeight * this.renderedRows / this.items.size());
        int thumbTop = this.listTop + (trackHeight - thumbHeight) * this.scrollOffset
                / Math.max(1, this.items.size() - this.renderedRows);
        int barX = this.x + this.renderWidth - 3;
        guiGraphics.fill(barX, this.listTop, barX + 2, listBottom, 0x30101010);
        guiGraphics.fill(barX, thumbTop, barX + 2, thumbTop + thumbHeight, 0x80CCCCCC);
    }

    /// 鼠标是否落在当前可见列表区域内（用于把滚轮交给下拉而不是场景缩放）。
    public boolean contains(double mouseX, double mouseY) {
        return this.open && this.renderedRows > 0
                && mouseX >= this.x && mouseX < this.x + this.renderWidth
                && mouseY >= this.listTop && mouseY < this.listTop + this.renderedRows * this.rowHeight;
    }

    /// 按滚轮方向滚动一行。`scrollY > 0` 向上（看更靠前的条目）。
    public void scroll(double scrollY) {
        if (!this.open || scrollY == 0.0D) {
            return;
        }
        int maxOffset = Math.max(0, this.items.size() - this.visibleCapacity);
        this.scrollOffset = Mth.clamp(this.scrollOffset + (scrollY > 0.0D ? -1 : 1), 0, maxOffset);
    }

    /// 命中的条目下标（基于上次 [#render] 的可见行与滚动位置），未命中返回 -1。
    public int hitTest(double mouseX, double mouseY) {
        if (!this.open || mouseX < this.x || mouseX >= this.x + this.renderWidth || mouseY < this.listTop) {
            return -1;
        }
        int row = (int) ((mouseY - this.listTop) / this.rowHeight);
        return row >= 0 && row < this.renderedRows ? this.scrollOffset + row : -1;
    }
}

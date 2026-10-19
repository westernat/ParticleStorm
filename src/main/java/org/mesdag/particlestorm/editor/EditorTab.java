package org.mesdag.particlestorm.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/// 左侧工具框的页签：每一页只负责自己那片内容区的渲染与交互。
///
/// 页签栏由 [EditorScreen] 绘制；点击页签切过去时先调用旧页签的 [#onHidden]，
/// 鼠标/键盘/滚轮事件只转发给当前页签。包内字段（`EditorScreen#effect`、`#error`、`#hoveredTooltip` 等）
/// 直接按包级可见性互访，不再额外包一层 getter/setter。
abstract class EditorTab {
    protected final EditorScreen screen;
    protected final Font font;

    protected EditorTab(EditorScreen screen, Font font) {
        this.screen = screen;
        this.font = font;
    }

    /// 页签标题。
    abstract Component title();

    /// 在工具框内容区（左上角 = 工具框左缘、页签栏下沿）绘制本页内容。
    abstract void render(GuiGraphics guiGraphics, int contentTop, int mouseX, int mouseY);

    /// 点击（坐标保证在工具框区域内）；返回 true 表示已消费。
    boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    /// 按住左键拖动（工具框内的拖拽交互，例如渐变色标）；返回 true 表示已消费。
    boolean mouseDragged(double mouseX, double mouseY) {
        return false;
    }

    /// 松开左键；返回 true 表示曾处于拖动状态。
    boolean mouseReleased(double mouseX, double mouseY) {
        return false;
    }

    /// 当前页签是否正在吃掉外部点击（编辑框中 / 下拉展开时）。
    boolean isCapturingInput() {
        return false;
    }

    /// 指针在展开的下拉列表上时让页签自己滚动列表；返回 true 表示已消费。
    boolean scroll(double mouseX, double mouseY, double scrollY) {
        return false;
    }

    /// 收起编辑框与下拉。
    void closeEditing() {
    }

    /// 切走时调用。
    void onHidden() {
        this.closeEditing();
    }

    /// 整份效果被替换（换粒子 / 新建）时调用：丢弃本页所有针对旧效果的编辑状态（打开的下拉、Grid、编辑框等）。
    ///
    /// 和 [#onHidden] 不同：这里不能把旧 Grid 的未提交内容再提交到新效果上，所以子类应直接丢弃状态。
    void onEffectReplaced() {
        this.closeEditing();
    }

    boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    boolean charTyped(char codePoint, int modifiers) {
        return false;
    }

    /// 某点是否落在展开的下拉列表（工具栏或本页）上：被列表盖住的文本不再触发 tooltip。
    protected boolean isOverOpenMenu(double mouseX, double mouseY) {
        return this.screen.isOverToolbarMenu(mouseX, mouseY);
    }

    /// 悬停在下拉条目且文本被截断时，记录完整文本作为 tooltip。
    protected void tooltipForMenu(DropdownWidget dropdown, int mouseX, int mouseY) {
        if (!dropdown.isOpen()) {
            return;
        }
        String full = dropdown.item(dropdown.hitTest(mouseX, mouseY));
        if (full != null && this.font.width(full) > dropdown.textWidth()) {
            this.screen.hoveredTooltip = Component.literal(full);
        }
    }
}

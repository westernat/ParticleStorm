package org.mesdag.particlestorm.editor;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.mesdag.particlestorm.data.description.DescriptionMaterial;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/// 纹理图集查看器（打开时盖住大部分屏幕）：按描述里的材质挑对应的图集，
/// 把图集里的 [TextureAtlasSprite] 列出来，点一个就把它绑给编辑器文档。
///
/// 交互照搬 AtlasViewer（XFactHD/AtlasViewer，`AtlasScreen`）的做法：
/// - **直接把整张图集贴图画出来**，滚轮缩放（`scale + delta * 0.1`，最小 1 倍「适应窗口」）、
///   **左键拖动平移**（拖动距离小于阈值才算点击）；
/// - 鼠标悬停的 sprite 画红框高亮（1px 外扩、裁到可视区里）、标题栏显示它的名字与像素尺寸；
/// - 搜索引擎（`SearchBox` / `SearchHandler`）：输入名字片段 → 匹配的 sprite 画橙框，
///   `ENTER` / `SHIFT+ENTER` 在匹配项之间跳转（当前项紫框闪烁）；
/// - 图集下拉 / 高亮动画 / 导出之类的功能不是编辑器需要的，没搬；另外加了一个**格子视图**方便挑小 sprite。
///
/// 图集按材质选：`terrain_sheet` → 方块图集，其它材质 → 粒子图集。
final class AtlasViewer {
    private static final int MARGIN = 16;
    /// 顶栏高度与内部两行（标题行 / 搜索行）的 y 偏移。
    private static final int HEADER_HEIGHT = 48;
    private static final int TITLE_Y = 6;
    private static final int SEARCH_Y = 26;
    private static final int SEARCH_WIDTH = 180;
    private static final int CELL_MIN = 12;
    private static final int CELL_MAX = 96;
    private static final float ZOOM_MIN = 1.0F;
    private static final float ZOOM_MAX = 32.0F;
    /// 滚轮每一格的缩放倍数（等比缩放）。
    private static final float ZOOM_STEP = 1.2F;
    private static final int DRAG_THRESHOLD = 3;
    private static final int COLOR_HOVER = 0xFFFF0000;
    private static final int COLOR_CURRENT = 0xFF30FF60;
    private static final int COLOR_MATCH = 0xFFFFBB00;
    private static final int COLOR_FOCUS = 0xFFCC00FF;

    private final EditorScreen screen;
    private final Font font;
    private final Consumer<TextureAtlasSprite> onSelect;
    private final ResourceLocation atlasLocation;
    private final List<TextureAtlasSprite> sprites;
    private final @Nullable ResourceLocation currentTexture;
    /// 图集贴图的像素尺寸（从 sprite 的图集像素坐标与 UV 反推）。
    private final int atlasWidth;
    private final int atlasHeight;
    private final MolangEditBox searchBox;

    private boolean gridMode;
    private int cellSize = 32;
    private int scroll;
    /// 相对「适应窗口」的缩放倍数，1 = 正好塞满可视区。
    private float zoom = 1.0F;
    private float panX;
    private float panY;
    private boolean dragging;
    private boolean dragMoved;
    private double pressX;
    private double pressY;
    /// 搜索匹配到的 sprite 下标（按图集里的 y/x 排序，和 AtlasViewer 一致）。
    private int[] matches = new int[0];
    private int focusedMatch = -1;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentHeight;
    private int columns = 1;
    private int toggleX;
    private int toggleWidth;
    private int toggleY;

    AtlasViewer(EditorScreen screen, Font font, DescriptionMaterial material, @Nullable ResourceLocation currentTexture,
                Consumer<TextureAtlasSprite> onSelect) {
        this.screen = screen;
        this.font = font;
        this.onSelect = onSelect;
        this.currentTexture = currentTexture;
        // 图集由材质自己声明（DescriptionMaterial 注册时带的 Supplier<TextureAtlas>）
        TextureAtlas atlas = material.getAtlas();
        this.atlasLocation = atlas.location();
        List<TextureAtlasSprite> list = atlas.getTextureLocations().stream().map(atlas::getSprite).toList();
        list.sort(Comparator.comparing(sprite -> sprite.contents().name().toString()));
        this.sprites = list;
        this.atlasWidth = deduceAtlasSize(true);
        this.atlasHeight = deduceAtlasSize(false);
        this.searchBox = new MolangEditBox(font, 0, 0, SEARCH_WIDTH, 14, Component.empty());
        this.searchBox.setMaxLength(128);
        this.searchBox.setFocused(true);
        this.layout();
        this.centerOnCurrent();
    }

    // region 静态工具

    /// 按「材质 → 图集」在该图集里按名字查 sprite（查不到返回 null）。
    static @Nullable TextureAtlasSprite spriteFor(DescriptionMaterial material, ResourceLocation texture) {
        return material.getAtlas().getSprite(texture);
    }

    /// 从 sprite 的图集像素坐标 / UV 反推图集贴图尺寸：`u0 = x / atlasW`。
    private int deduceAtlasSize(boolean horizontal) {
        for (TextureAtlasSprite sprite : this.sprites) {
            int pixel = horizontal ? sprite.getX() : sprite.getY();
            float uv0 = horizontal ? sprite.getU0() : sprite.getV0();
            float uv1 = horizontal ? sprite.getU1() : sprite.getV1();
            if (pixel > 0 && uv0 > 0.0F) {
                return Math.max(1, Math.round(pixel / uv0));
            }
            if (uv1 > uv0) {
                int size = horizontal ? sprite.contents().width() : sprite.contents().height();
                return Math.max(1, Math.round(size / (uv1 - uv0)));
            }
        }
        int max = 1;
        for (TextureAtlasSprite sprite : this.sprites) {
            int pixel = horizontal ? sprite.getX() + sprite.contents().width() : sprite.getY() + sprite.contents().height();
            max = Math.max(max, pixel);
        }
        return max;
    }

    /// 打开时把视角对到当前贴图上。
    private void centerOnCurrent() {
        if (this.currentTexture == null) {
            return;
        }
        for (int i = 0; i < this.sprites.size(); i++) {
            TextureAtlasSprite sprite = this.sprites.get(i);
            if (!sprite.contents().name().equals(this.currentTexture)) {
                continue;
            }
            this.zoom = Math.min(8.0F, ZOOM_MAX);
            this.centerSprite(sprite);
            this.scrollToCell(i);
            return;
        }
    }

    // endregion

    // region 布局

    /// 面板布局：顶栏两行（标题 + 悬停/搜索状态；搜索框 + 提示 + 视图切换），下面是内容区。
    /// 所有坐标都在这里算好，渲染和命中判定共用，避免两边各写一份导致对不齐。
    private void layout() {
        this.panelX = MARGIN;
        this.panelY = MARGIN;
        this.panelWidth = Math.max(160, this.screen.width - MARGIN * 2);
        this.panelHeight = Math.max(120, this.screen.height - MARGIN * 2);
        this.contentX = this.panelX + 4;
        this.contentY = this.panelY + HEADER_HEIGHT;
        this.contentWidth = this.panelWidth - 8;
        this.contentHeight = Math.max(20, this.panelHeight - HEADER_HEIGHT - 4);
        this.columns = Math.max(1, this.contentWidth / this.cellSize);
        this.toggleWidth = this.font.width(this.toggleLabel()) + 8;
        this.toggleX = this.panelX + this.panelWidth - 6 - this.toggleWidth;
        this.toggleY = this.panelY + SEARCH_Y + 1;
        int searchSpace = this.toggleX - (this.panelX + 6) - 10;
        this.searchBox.setX(this.panelX + 6);
        this.searchBox.setY(this.panelY + SEARCH_Y);
        this.searchBox.setWidth(Math.max(60, Math.min(SEARCH_WIDTH, searchSpace)));
    }

    private String toggleLabel() {
        return this.gridMode
                ? Component.translatable("particle_editor.texture.view_atlas").getString()
                : Component.translatable("particle_editor.texture.view_grid").getString();
    }

    private float fitScale() {
        if (this.atlasWidth <= 0 || this.atlasHeight <= 0) {
            return 0.0F;
        }
        return Math.min((float) this.contentWidth / this.atlasWidth, (float) this.contentHeight / this.atlasHeight);
    }

    private void clampPan() {
        float scale = this.fitScale() * this.zoom;
        if (scale <= 0.0F) {
            this.panX = 0.0F;
            this.panY = 0.0F;
            return;
        }
        float limitX = Math.max(0.0F, (this.atlasWidth * scale - this.contentWidth) * 0.5F);
        float limitY = Math.max(0.0F, (this.atlasHeight * scale - this.contentHeight) * 0.5F);
        this.panX = Mth.clamp(this.panX, -limitX, limitX);
        this.panY = Mth.clamp(this.panY, -limitY, limitY);
    }

    private boolean isOverPanel(double mouseX, double mouseY) {
        return mouseX >= this.panelX && mouseX < this.panelX + this.panelWidth
                && mouseY >= this.panelY && mouseY < this.panelY + this.panelHeight;
    }

    private boolean isOverContent(double mouseX, double mouseY) {
        return mouseX >= this.contentX && mouseX < this.contentX + this.contentWidth
                && mouseY >= this.contentY && mouseY < this.contentY + this.contentHeight;
    }

    private boolean isOverToggle(double mouseX, double mouseY) {
        return mouseX >= this.toggleX && mouseX < this.toggleX + this.toggleWidth
                && mouseY >= this.toggleY && mouseY < this.toggleY + 12;
    }

    // endregion

    // region 图集视图坐标

    /// 图集贴图在屏幕上的绘制矩形：`{x, y, w, h, 缩放}`。
    private float @Nullable[] imageRect() {
        float fit = this.fitScale();
        if (fit <= 0.0F) {
            return null;
        }
        float scale = fit * this.zoom;
        float drawWidth = this.atlasWidth * scale;
        float drawHeight = this.atlasHeight * scale;
        float x = this.contentX + (this.contentWidth - drawWidth) * 0.5F + this.panX;
        float y = this.contentY + (this.contentHeight - drawHeight) * 0.5F + this.panY;
        return new float[]{x, y, drawWidth, drawHeight, scale};
    }

    private int spriteAtCursor(double mouseX, double mouseY) {
        if (!this.isOverContent(mouseX, mouseY)) {
            return -1;
        }
        float[] rect = this.imageRect();
        if (rect == null || rect[4] <= 0.0F) {
            return -1;
        }
        float imageX = (float) ((mouseX - rect[0]) / rect[4]);
        float imageY = (float) ((mouseY - rect[1]) / rect[4]);
        if (imageX < 0.0F || imageY < 0.0F || imageX >= this.atlasWidth || imageY >= this.atlasHeight) {
            return -1;
        }
        for (int i = 0; i < this.sprites.size(); i++) {
            TextureAtlasSprite sprite = this.sprites.get(i);
            if (imageX >= sprite.getX() && imageX < sprite.getX() + sprite.contents().width()
                    && imageY >= sprite.getY() && imageY < sprite.getY() + sprite.contents().height()) {
                return i;
            }
        }
        return -1;
    }

    /// 把某个 sprite 移到可视区中心（图集视图）。
    private void centerSprite(TextureAtlasSprite sprite) {
        float fit = this.fitScale();
        if (fit <= 0.0F) {
            return;
        }
        float scale = fit * this.zoom;
        float imageWidth = this.atlasWidth * scale;
        float imageHeight = this.atlasHeight * scale;
        float spriteX = sprite.getX() + sprite.contents().width() * 0.5F;
        float spriteY = sprite.getY() + sprite.contents().height() * 0.5F;
        this.panX = imageWidth * 0.5F - spriteX * scale;
        this.panY = imageHeight * 0.5F - spriteY * scale;
        this.clampPan();
    }

    // endregion

    // region 格子视图坐标

    private int contentRowsHeight() {
        int rows = (this.sprites.size() + this.columns - 1) / this.columns;
        return rows * this.cellSize;
    }

    private void clampScroll() {
        this.scroll = Mth.clamp(this.scroll, 0, Math.max(0, this.contentRowsHeight() - this.contentHeight));
    }

    private int cellAtCursor(double mouseX, double mouseY) {
        if (!this.isOverContent(mouseX, mouseY)) {
            return -1;
        }
        int column = (int) ((mouseX - this.contentX) / this.cellSize);
        int row = (int) ((mouseY - this.contentY + this.scroll) / this.cellSize);
        int index = row * this.columns + column;
        return index >= 0 && index < this.sprites.size() ? index : -1;
    }

    private void scrollToCell(int index) {
        int row = index / Math.max(1, this.columns);
        this.scroll = Math.max(0, row * this.cellSize - this.contentHeight / 2 + this.cellSize / 2);
        this.clampScroll();
    }

    // endregion

    private int indexAt(double mouseX, double mouseY) {
        return this.gridMode ? this.cellAtCursor(mouseX, mouseY) : this.spriteAtCursor(mouseX, mouseY);
    }

    private int indexOfCurrent() {
        if (this.currentTexture == null) {
            return -1;
        }
        for (int i = 0; i < this.sprites.size(); i++) {
            if (this.sprites.get(i).contents().name().equals(this.currentTexture)) {
                return i;
            }
        }
        return -1;
    }

    // region 搜索

    /// 重新计算搜索匹配（输入变化时调用）。排序与 AtlasViewer 一致：先 y 后 x。
    private void refreshMatches() {
        String query = this.searchBox.getValue();
        if (query.isBlank()) {
            this.matches = new int[0];
            this.focusedMatch = -1;
            return;
        }
        List<Integer> found = new ArrayList<>();
        for (int i = 0; i < this.sprites.size(); i++) {
            if (this.sprites.get(i).contents().name().toString().contains(query)) {
                found.add(i);
            }
        }
        found.sort(Comparator.comparingInt((Integer i) -> this.sprites.get(i).getY())
                .thenComparingInt(i -> this.sprites.get(i).getX()));
        this.matches = found.stream().mapToInt(Integer::intValue).toArray();
        this.focusedMatch = this.matches.length == 0 ? -1 : 0;
        if (this.focusedMatch >= 0) {
            this.jumpToMatch(this.matches[this.focusedMatch]);
        }
    }

    private void jumpToNextMatch(int direction) {
        if (this.matches.length == 0) {
            return;
        }
        int size = this.matches.length;
        this.focusedMatch = Math.floorMod(this.focusedMatch + direction, size);
        this.jumpToMatch(this.matches[this.focusedMatch]);
    }

    private void jumpToMatch(int spriteIndex) {
        if (spriteIndex < 0 || spriteIndex >= this.sprites.size()) {
            return;
        }
        TextureAtlasSprite sprite = this.sprites.get(spriteIndex);
        if (this.gridMode) {
            this.scrollToCell(spriteIndex);
        } else {
            this.zoom = Math.max(this.zoom, 1.0F);
            this.centerSprite(sprite);
        }
    }

    private boolean isMatch(int index) {
        for (int match : this.matches) {
            if (match == index) {
                return true;
            }
        }
        return false;
    }

    private boolean isFocusedMatch(int index) {
        return this.focusedMatch >= 0 && this.focusedMatch < this.matches.length && this.matches[this.focusedMatch] == index;
    }

    // endregion

    // region 渲染

    void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        this.layout();
        this.clampScroll();
        this.clampPan();
        int hovered = this.indexAt(mouseX, mouseY);
        int current = this.indexOfCurrent();

        // 图集贴图本来是带 alpha 的，必须保证当前是「默认 alpha 混合」而不是粒子的 ADD 混合
        // （编辑器场景 pass 里 PARTICLE_ADD 会 `blendFunc(SRC_ALPHA, ONE)` 且不会还回去）
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        guiGraphics.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xF8101418);
        guiGraphics.renderOutline(this.panelX, this.panelY, this.panelWidth, this.panelHeight, 0xFF4A5A7A);

        // 第一行：标题（左侧）+ 悬停的 sprite / 搜索状态（右侧）；两边都按可用宽度截断
        String title = Component.translatable("particle_editor.texture.title", this.atlasLocation.toString(), this.sprites.size()).getString();
        String status = hovered >= 0 ? this.spriteLabel(this.sprites.get(hovered)) : this.searchStatus();
        int statusWidth = status.isEmpty() ? 0 : Math.min(this.font.width(status), this.panelWidth / 2 - 12);
        String titleText = this.font.plainSubstrByWidth(title, Math.max(20, this.panelWidth - 12 - statusWidth - 8));
        guiGraphics.drawString(this.font, titleText, this.panelX + 6, this.panelY + TITLE_Y, 0xFFFFFF, false);
        if (statusWidth > 0) {
            String statusText = this.font.plainSubstrByWidth(status, statusWidth);
            guiGraphics.drawString(this.font, statusText, this.panelX + this.panelWidth - 6 - this.font.width(statusText),
                    this.panelY + TITLE_Y, hovered >= 0 ? 0x9CDCFE : 0xBBBBBB, false);
        }

        // 第二行：搜索框（左）+ 提示（中间，放不下就省略）+ 视图切换按钮（右）
        this.searchBox.render(guiGraphics, mouseX, mouseY, 0);
        boolean toggleHover = this.isOverToggle(mouseX, mouseY);
        guiGraphics.fill(this.toggleX, this.toggleY, this.toggleX + this.toggleWidth, this.toggleY + 12,
                toggleHover ? 0xFF3A3A48 : 0xFF2A2A34);
        guiGraphics.renderOutline(this.toggleX, this.toggleY, this.toggleWidth, 12, 0xFF4A4A58);
        guiGraphics.drawString(this.font, this.toggleLabel(), this.toggleX + 4, this.toggleY + 2, 0xFFFFFF, false);

        // 顶栏与内容区之间的分隔线
        guiGraphics.fill(this.panelX + 1, this.contentY - 3, this.panelX + this.panelWidth - 1, this.contentY - 2, 0xFF2F3A4A);

        guiGraphics.enableScissor(this.contentX, this.contentY, this.contentX + this.contentWidth,
                this.contentY + this.contentHeight);
        try {
            if (this.sprites.isEmpty()) {
                String empty = Component.translatable("particle_editor.texture.empty").getString();
                guiGraphics.drawString(this.font, empty, this.contentX + 4, this.contentY + 4, 0x777777, false);
            } else if (this.gridMode) {
                this.renderGrid(guiGraphics, hovered, current);
            } else {
                this.renderAtlasImage(guiGraphics, hovered, current);
            }
        } finally {
            guiGraphics.disableScissor();
        }
    }

    private String spriteLabel(TextureAtlasSprite sprite) {
        return sprite.contents().name() + "  (" + sprite.contents().width() + "×" + sprite.contents().height() + ")";
    }

    private String searchStatus() {
        if (this.searchBox.getValue().isBlank()) {
            return "";
        }
        int count = this.matches.length;
        if (count == 0) {
            return Component.translatable("particle_editor.texture.no_match").getString();
        }
        return (this.focusedMatch + 1) + " / " + count;
    }

    /// 图集贴图 / sprite 都是带 alpha 的，而 GUI 的 blit 底层（`GuiGraphics#innerBlit`）既不管混合函数，
    /// 16/20 参数那个重载画完还会 `disableBlend()`。这里直接压 GL 状态（`_` 前缀方法会真正下发 GL 指令，
    /// 不受 `GlStateManager` 缓存影响），保证图片按「src.a / 1-src.a」混合，而不是被当成不透明贴图。
    private static void ensureAlphaBlend() {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
    }

    /// 图集视图：整张图集贴图 + 悬停/已绑定/搜索匹配的高亮框。
    private void renderAtlasImage(GuiGraphics guiGraphics, int hovered, int current) {
        float[] rect = this.imageRect();
        if (rect == null) {
            return;
        }
        ensureAlphaBlend();
        int x = Math.round(rect[0]);
        int y = Math.round(rect[1]);
        int width = Math.max(1, Math.round(rect[2]));
        int height = Math.max(1, Math.round(rect[3]));
        float scale = rect[4];
        guiGraphics.blit(this.atlasLocation, x, y, width, height, 0.0F, 0.0F,
                this.atlasWidth, this.atlasHeight, this.atlasWidth, this.atlasHeight);
        for (int match : this.matches) {
            if (match != current && match != hovered) {
                this.outlineSprite(guiGraphics, this.sprites.get(match), x, y, scale, COLOR_MATCH);
            }
        }
        if (this.focusedMatch >= 0 && this.focusedMatch < this.matches.length) {
            int focused = this.matches[this.focusedMatch];
            if (focused != current && focused != hovered) {
                boolean blink = (System.currentTimeMillis() / 200L) % 2L == 0L;
                this.outlineSprite(guiGraphics, this.sprites.get(focused), x, y, scale, blink ? COLOR_FOCUS : COLOR_MATCH);
            }
        }
        if (current >= 0) {
            this.outlineSprite(guiGraphics, this.sprites.get(current), x, y, scale, COLOR_CURRENT);
        }
        if (hovered >= 0 && hovered != current) {
            this.outlineSprite(guiGraphics, this.sprites.get(hovered), x, y, scale, COLOR_HOVER);
        }
    }

    /// 在屏幕上给一个 sprite 画 1px 边框（外扩 1px，和 AtlasViewer 的 `drawColoredBox(..., expand)` 一致）。
    private void outlineSprite(GuiGraphics guiGraphics, TextureAtlasSprite sprite, int imageX, int imageY,
                               float scale, int color) {
        int x = imageX + Math.round(sprite.getX() * scale) - 1;
        int y = imageY + Math.round(sprite.getY() * scale) - 1;
        int width = Math.max(2, Math.round(sprite.contents().width() * scale) + 2);
        int height = Math.max(2, Math.round(sprite.contents().height() * scale) + 2);
        guiGraphics.fill(x, y, x + width, y + 1, color);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
        guiGraphics.fill(x, y, x + 1, y + height, color);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    /// 格子视图：每个 sprite 单独放大成一格（按名字排序）。
    private void renderGrid(GuiGraphics guiGraphics, int hovered, int current) {
        int cell = this.cellSize;
        for (int i = 0; i < this.sprites.size(); i++) {
            int column = i % this.columns;
            int row = i / this.columns;
            int x = this.contentX + column * cell;
            int y = this.contentY + row * cell - this.scroll;
            if (y + cell <= this.contentY || y >= this.contentY + this.contentHeight) {
                continue;
            }
            // sprite 带 alpha：每个格子画之前都重压一次混合状态
            ensureAlphaBlend();
            if (i == hovered) {
                guiGraphics.fill(x, y, x + cell - 1, y + cell - 1, 0x30FFFFFF);
                // `fill` 走 `RenderType.gui()`，顶点先攒在 BufferSource 里，要等 flush 才真正绘制
                // （这一帧里最近的一次 flush 在 `disableScissor`），而下面 sprite 的 `blit` 是**立即绘制**：
                // 不主动 flush 的话，高亮底的绘制顺序会排到 sprite 之后，把悬停 sprite 的透明像素糊成白色。
                guiGraphics.flush();
                // flush 画完 `RenderType.gui()` 的 `clearRenderState` 会把混合关掉，blit 之前得再压一次
                ensureAlphaBlend();
            }
            guiGraphics.blit(x + 1, y + 1, 0, cell - 2, cell - 2, this.sprites.get(i));
            int color = i == current ? COLOR_CURRENT
                    : isFocusedMatch(i) ? COLOR_FOCUS
                    : isMatch(i) ? COLOR_MATCH
                    : i == hovered ? 0xFFFFFFFF : 0x40FFFFFF;
            guiGraphics.renderOutline(x, y, cell - 1, cell - 1, color);
        }
    }

    // endregion

    // region 交互

    void mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isOverPanel(mouseX, mouseY)) {
            this.screen.closeAtlasViewer();
            return;
        }
        if (this.searchBox.isMouseOver(mouseX, mouseY)) {
            this.searchBox.mouseClicked(mouseX, mouseY, 0);
            return;
        }
        this.searchBox.setFocused(true);
        if (button == 0 && this.isOverToggle(mouseX, mouseY)) {
            this.gridMode = !this.gridMode;
            return;
        }
        if (button != 0) {
            return;
        }
        if (this.isOverContent(mouseX, mouseY)) {
            // 左键按下：先当作「可能要拖动平移」，松手时没拖动才算点击绑定（和 AtlasViewer 的左键拖动一致）
            this.dragging = true;
            this.dragMoved = false;
            this.pressX = mouseX;
            this.pressY = mouseY;
        }
    }

    void mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.dragging || button != 0 || this.gridMode) {
            return;
        }
        if (Math.abs(mouseX - this.pressX) > DRAG_THRESHOLD || Math.abs(mouseY - this.pressY) > DRAG_THRESHOLD) {
            this.dragMoved = true;
        }
        if (this.dragMoved) {
            this.panX += (float) dragX;
            this.panY += (float) dragY;
        }
    }

    void mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0 || !this.dragging) {
            return;
        }
        this.dragging = false;
        if (this.dragMoved || !this.isOverContent(mouseX, mouseY)) {
            return;
        }
        int index = this.indexAt(mouseX, mouseY);
        if (index < 0) {
            return;
        }
        TextureAtlasSprite sprite = this.sprites.get(index);
        this.screen.closeAtlasViewer();
        this.onSelect.accept(sprite);
    }

    void scroll(double mouseX, double mouseY, double scrollY) {
        if (!this.isOverContent(mouseX, mouseY)) {
            return;
        }
        if (this.gridMode) {
            if (Screen.hasControlDown()) {
                int old = this.cellSize;
                this.cellSize = Mth.clamp(this.cellSize + (scrollY > 0 ? 8 : -8), CELL_MIN, CELL_MAX);
                if (this.cellSize != old) {
                    this.layout();
                    this.scroll = Math.max(0, this.scroll * this.cellSize / old);
                }
            } else {
                this.scroll -= (int) (scrollY * Math.max(8, this.cellSize));
            }
            this.clampScroll();
            return;
        }
        // 图集视图：等比缩放（每格滚轮 ×1.2 / ÷1.2），这样高倍率下也不会「越放大越难放大」
        float oldZoom = this.zoom;
        float factor = (float) Math.pow(ZOOM_STEP, scrollY);
        this.zoom = Mth.clamp(this.zoom * factor, ZOOM_MIN, ZOOM_MAX);
        if (this.zoom == oldZoom) {
            return;
        }
        float anchorX = (float) mouseX - (this.contentX + this.contentWidth * 0.5F);
        float anchorY = (float) mouseY - (this.contentY + this.contentHeight * 0.5F);
        float ratio = this.zoom / oldZoom;
        this.panX = anchorX - (anchorX - this.panX) * ratio;
        this.panY = anchorY - (anchorY - this.panY) * ratio;
    }

    void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.screen.closeAtlasViewer();
            return;
        }
        if (keyCode == 257 || keyCode == 335) { // ENTER / KP_ENTER：跳到下一个匹配
            this.jumpToNextMatch(Screen.hasShiftDown() ? -1 : 1);
            return;
        }
        if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            this.refreshMatches();
        }
    }

    boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox.charTyped(codePoint, modifiers)) {
            this.refreshMatches();
        }
        return true;
    }

    // endregion
}

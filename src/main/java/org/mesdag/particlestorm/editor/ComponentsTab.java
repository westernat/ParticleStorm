package org.mesdag.particlestorm.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.IComponent;
import org.mesdag.particlestorm.api.RegisterComponentGridEvent;
import org.mesdag.particlestorm.data.DefinedParticleEffect;

import java.util.*;

/// 「组件」页签：按**组件族**（[RegisterComponentGridEvent.Entry]）列出效果里的组件。
///
/// 族的特性决定交互：
/// - 同族多个子类型（`emitter_rate` / `emitter_lifetime` / `emitter_shape`）：列表一行，进 Grid 后用「类型」下拉切换子类型；
/// - `required` 组件必须存在，Grid 里不显示「删除」行；
/// - 其它组件可选，用「添加组件」下拉增加、Grid 里删除；
/// - 修改一律走 `Grid#build()` → 从 `effect.components` 移除同族全部 id → 放入新 id 的实例 → 清场景重新发射。
final class ComponentsTab extends EditorTab {
    private static final int LIST_ROW_HEIGHT = 12;

    /// 列表里的一族：已知族才有 `entry`，未注册 Grid 的组件为只读展示行。
    record Family(@Nullable RegisterComponentGridEvent.Entry entry, List<ResourceLocation> present) {
    }

    private final DropdownWidget addDropdown;
    private @Nullable RegisterComponentGridEvent.Entry selectedEntry;
    private @Nullable ComponentGrid grid;
    /// Grid 内容超出可视区时的滚动偏移（像素）。
    private int gridScroll;
    private List<Family> families = List.of();
    private List<RegisterComponentGridEvent.Entry> addEntries = List.of();
    /// 「添加组件」候选按效果实例缓存（渲染时不再每帧 new Grid 询 canAdd）。
    private @Nullable DefinedParticleEffect candidatesFor;
    private List<RegisterComponentGridEvent.Entry> candidatesCache = List.of();
    private int addRowTop;
    private int removeRowTop;

    ComponentsTab(EditorScreen screen, Font font) {
        super(screen, font);
        this.addDropdown = new DropdownWidget(2, 0, Math.max(20, screen.toolboxWidth() - 4), LIST_ROW_HEIGHT);
    }

    @Override
    Component title() {
        return Component.translatable("particle_editor.tab.components");
    }

    @Override
    void render(GuiGraphics guiGraphics, int contentTop, int mouseX, int mouseY) {
        DefinedParticleEffect effect = this.screen.effect;
        if (effect == null) {
            guiGraphics.drawString(this.font, Component.translatable("particle_editor.error.no_particle"), 6, contentTop + 2, 0x888888, false);
            return;
        }
        this.families = collectFamilies(effect);
        if (this.selectedEntry != null && !isPresent(effect, this.selectedEntry)) {
            this.closeGrid();
        }
        if (this.grid != null) {
            this.renderGrid(guiGraphics, contentTop, mouseX, mouseY);
        } else {
            this.renderList(guiGraphics, contentTop, mouseX, mouseY);
        }
    }

    private List<Family> collectFamilies(DefinedParticleEffect effect) {
        Map<RegisterComponentGridEvent.Entry, List<ResourceLocation>> byEntry = new LinkedHashMap<>();
        List<Family> result = new ArrayList<>();
        for (ResourceLocation id : effect.components.keySet()) {
            RegisterComponentGridEvent.Entry entry = RegisterComponentGridEvent.entryFor(id);
            if (entry == null) {
                result.add(new Family(null, List.of(id)));
            } else {
                byEntry.computeIfAbsent(entry, key -> new ArrayList<>()).add(id);
            }
        }
        byEntry.forEach((entry, present) -> {
            List<ResourceLocation> ordered = entry.variants().stream().filter(present::contains).toList();
            result.add(new Family(entry, ordered));
        });
        result.sort(Comparator.comparingInt(family -> family.present().stream()
                .mapToInt(id -> effect.components.get(id).order()).min().orElse(Integer.MAX_VALUE)));
        return result;
    }

    private void renderList(GuiGraphics guiGraphics, int contentTop, int mouseX, int mouseY) {
        int width = this.screen.toolboxWidth();
        int y = contentTop + 2;
        for (Family family : this.families) {
            if (y + LIST_ROW_HEIGHT > this.screen.height - 2) {
                break;
            }
            boolean hover = mouseX >= 0 && mouseX < width && mouseY >= y && mouseY < y + LIST_ROW_HEIGHT
                    && !this.isOverOpenMenu(mouseX, mouseY);
            if (hover) {
                guiGraphics.fill(2, y, width - 4, y + LIST_ROW_HEIGHT, 0x20FFFFFF);
            }
            guiGraphics.drawString(this.font, this.screen.truncatedFor(labelOf(family), width - 12, hover), 6, y, 0xCCCCCC, false);
            y += LIST_ROW_HEIGHT;
        }
        this.addRowTop = y + 4;
        boolean addHover = mouseX >= 2 && mouseX < width - 4 && mouseY >= this.addRowTop && mouseY < this.addRowTop + LIST_ROW_HEIGHT;
        if (addHover) {
            guiGraphics.fill(2, this.addRowTop, width - 4, this.addRowTop + LIST_ROW_HEIGHT, 0x20FFFFFF);
        }
        guiGraphics.drawString(this.font,
                "+ " + Component.translatable("particle_editor.component.add").getString(),
                6, this.addRowTop + 2, 0xFFFFFF, false);
        this.screen.renderDropdownArrow(guiGraphics, width - 13, this.addRowTop + 5);
        this.addEntries = this.addCandidates();
        this.addDropdown.setItems(this.addEntries.stream().map(ComponentsTab::labelOf).toList());
        this.addDropdown.setPosition(2, this.addRowTop + LIST_ROW_HEIGHT);
        this.addDropdown.render(guiGraphics, this.font, mouseX, mouseY, this.screen.height);
        this.tooltipForMenu(this.addDropdown, mouseX, mouseY);
    }

    private String labelOf(Family family) {
        if (family.entry() == null) {
            return componentLabel(family.present().getFirst());
        }
        return labelOf(family.entry());
    }

    /// 组件族名：多子类型族（emitter_shape / emitter_rate / emitter_lifetime）显示族名（如 "Emitter Shape"），
    /// 单 id 组件显示自身组件名。
    static String labelOf(RegisterComponentGridEvent.Entry entry) {
        if (entry.variants().size() > 1) {
            ResourceLocation base = entry.defaultId();
            return Component.translatable("particle_editor." + base.getNamespace() + "." + entry.label()).getString();
        }
        return componentLabel(entry.defaultId());
    }

    /// 组件名/行标签的翻译键：`particlestorm.editor.<namespace>.<path>`（未翻译时原样显示键）。
    static String componentLabel(ResourceLocation id) {
        return Component.translatable("particle_editor." + id.getNamespace() + "." + id.getPath()).getString();
    }

    private void renderGrid(GuiGraphics guiGraphics, int contentTop, int mouseX, int mouseY) {
        ComponentGrid active = this.grid;
        if (active == null) {
            return;
        }
        int width = this.screen.toolboxWidth();
        boolean backHover = mouseX >= 2 && mouseX < width - 4 && mouseY >= contentTop && mouseY < contentTop + ComponentGrid.ROW_HEIGHT;
        if (backHover) {
            guiGraphics.fill(2, contentTop, width - 4, contentTop + ComponentGrid.ROW_HEIGHT, 0x20FFFFFF);
        }
        String back = "< " + Component.translatable("particle_editor.component.back").getString()
                + "  " + (this.selectedEntry != null ? labelOf(this.selectedEntry) : "");
        guiGraphics.drawString(this.font, this.screen.truncatedFor(back, width - 12, backHover), 6, contentTop + 4, 0xFFFFFF, false);

        int rowsTop = contentTop + ComponentGrid.ROW_HEIGHT;
        int viewBottom = this.screen.height - 2;
        int content = active.height() + this.gridExtraHeight();
        int maxScroll = Math.max(0, content - (viewBottom - rowsTop));
        this.gridScroll = Mth.clamp(this.gridScroll, 0, maxScroll);
        int drawTop = rowsTop - this.gridScroll;
        // 内容超出屏幕高度时可以滚动：超出可视区的部分被裁剪，「返回」行固定在顶部
        guiGraphics.enableScissor(0, rowsTop, width, viewBottom);
        try {
            // 删除行先画：展开的下拉列表（在 grid.render 里最后绘制）要能盖住它
            this.removeRowTop = drawTop + active.height() + 4;
            if (this.selectedEntry != null && !this.selectedEntry.required()) {
                boolean removeHover = mouseX >= 2 && mouseX < width - 4 && mouseY >= this.removeRowTop && mouseY < this.removeRowTop + ComponentGrid.ROW_HEIGHT;
                if (removeHover) {
                    guiGraphics.fill(2, this.removeRowTop, width - 4, this.removeRowTop + ComponentGrid.ROW_HEIGHT, 0x30FF0000);
                }
                guiGraphics.drawString(this.font,
                        Component.translatable("particle_editor.component.remove").getString(),
                        6, this.removeRowTop + 4, 0xFF7777, false);
            }
            active.render(guiGraphics, drawTop, mouseX, mouseY);
        } finally {
            guiGraphics.disableScissor();
        }
    }

    /// Grid 下方（滚动内容的一部分）额外高度：「删除组件」行的间距 + 行高。
    private int gridExtraHeight() {
        return this.selectedEntry != null && !this.selectedEntry.required() ? ComponentGrid.ROW_HEIGHT + 4 : 0;
    }

    @Override
    boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.grid != null ? this.clickGrid(mouseX, mouseY) : this.clickList(mouseX, mouseY);
    }

    @Override
    boolean mouseDragged(double mouseX, double mouseY) {
        if (this.grid == null) {
            return false;
        }
        boolean handled = this.grid.mouseDragged(mouseX, mouseY);
        this.applyPendingChanges();
        return handled;
    }

    @Override
    boolean mouseReleased(double mouseX, double mouseY) {
        if (this.grid == null) {
            return false;
        }
        boolean handled = this.grid.mouseReleased(mouseX, mouseY);
        this.applyPendingChanges();
        return handled;
    }

    /// 拖拽（调色盘 / 渐变色标）过程中也会改数据：变化后立刻重建组件并重新发射。
    private void applyPendingChanges() {
        if (this.grid != null && this.grid.consumeChanged()) {
            this.applyGrid();
        }
    }

    private boolean clickList(double mouseX, double mouseY) {
        if (this.addDropdown.isOpen()) {
            int index = this.addDropdown.hitTest(mouseX, mouseY);
            this.addDropdown.close();
            if (index >= 0 && index < this.addEntries.size()) {
                this.add(this.addEntries.get(index));
            }
            return true;
        }
        int width = this.screen.toolboxWidth();
        int y = this.screen.toolboxContentTop() + 2;
        for (Family family : this.families) {
            if (mouseX >= 0 && mouseX < width && mouseY >= y && mouseY < y + LIST_ROW_HEIGHT) {
                this.openGrid(family);
                return true;
            }
            y += LIST_ROW_HEIGHT;
        }
        if (mouseX >= 2 && mouseX < width - 4 && mouseY >= this.addRowTop && mouseY < this.addRowTop + LIST_ROW_HEIGHT) {
            this.screen.toggleMenu(this.addDropdown);
            return true;
        }
        return false;
    }

    private boolean clickGrid(double mouseX, double mouseY) {
        ComponentGrid active = this.grid;
        if (active == null) {
            return false;
        }
        int width = this.screen.toolboxWidth();
        int contentTop = this.screen.toolboxContentTop();
        if (mouseX >= 0 && mouseX < width && mouseY >= contentTop && mouseY < contentTop + ComponentGrid.ROW_HEIGHT) {
            this.closeGrid();
            return true;
        }
        // Grid 在前（展开的下拉列表可能盖住删除行），没消费再判删除行
        try {
            boolean handled = active.mouseClicked(mouseX, mouseY);
            if (active.consumeChanged()) {
                this.applyGrid();
            }
            if (handled) {
                return true;
            }
        } catch (RuntimeException e) {
            this.screen.error = Component.literal(String.valueOf(e.getMessage()));
            return true;
        }
        if (this.selectedEntry != null && !this.selectedEntry.required()
                && mouseX >= 0 && mouseX < width && mouseY >= this.removeRowTop && mouseY < this.removeRowTop + ComponentGrid.ROW_HEIGHT) {
            this.remove();
            return true;
        }
        return false;
    }

    @Override
    boolean isCapturingInput() {
        return this.addDropdown.isOpen() || (this.grid != null && this.grid.isCapturingInput());
    }

    @Override
    boolean scroll(double mouseX, double mouseY, double scrollY) {
        if (this.addDropdown.contains(mouseX, mouseY)) {
            this.addDropdown.scroll(scrollY);
            return true;
        }
        ComponentGrid active = this.grid;
        if (active == null) {
            return false;
        }
        int viewTop = this.screen.toolboxContentTop() + ComponentGrid.ROW_HEIGHT;
        int viewBottom = this.screen.height - 2;
        if (mouseX < 0 || mouseX >= this.screen.toolboxWidth() || mouseY < viewTop || mouseY >= viewBottom) {
            return false;
        }
        // 展开的下拉列表自己能滚（SelectRow），先让它消费
        if (active.scroll(mouseX, mouseY, scrollY)) {
            return true;
        }
        int content = active.height() + this.gridExtraHeight();
        int maxScroll = Math.max(0, content - (viewBottom - viewTop));
        this.gridScroll = Mth.clamp(this.gridScroll - (int) (scrollY * 12), 0, maxScroll);
        return true;
    }

    @Override
    void closeEditing() {
        this.addDropdown.close();
        if (this.grid != null) {
            this.grid.closeEditing();
            // 收尾时可能提交了调色盘读数格子的输入：立刻生效
            this.applyPendingChanges();
        }
    }

    /// 效果被整份替换：直接丢掉打开的 Grid / 下拉，避免把旧粒子的组件数据显示在新粒子上。
    @Override
    void onEffectReplaced() {
        this.addDropdown.close();
        if (this.grid != null) {
            // 只收起编辑状态，不消费 changed——旧 Grid 直接丢弃，不能把它的改动应用到新效果
            this.grid.closeEditing();
        }
        this.grid = null;
        this.selectedEntry = null;
        this.gridScroll = 0;
    }

    @Override
    boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.grid != null) {
            try {
                boolean handled = this.grid.keyPressed(keyCode, scanCode, modifiers);
                if (this.grid.consumeChanged()) {
                    this.applyGrid();
                }
                if (handled) {
                    return true;
                }
            } catch (RuntimeException e) {
                this.screen.error = Component.literal(String.valueOf(e.getMessage()));
                return true;
            }
            if (keyCode == 256 && this.grid.hasOpenMenus()) {
                this.grid.closeEditing();
                return true;
            }
        }
        if (keyCode == 256 && this.addDropdown.isOpen()) {
            this.addDropdown.close();
            return true;
        }
        return false;
    }

    @Override
    boolean charTyped(char codePoint, int modifiers) {
        if (this.grid == null) {
            return false;
        }
        try {
            return this.grid.charTyped(codePoint, modifiers);
        } catch (RuntimeException e) {
            this.screen.error = Component.literal(String.valueOf(e.getMessage()));
            return true;
        }
    }

    @Override
    protected boolean isOverOpenMenu(double mouseX, double mouseY) {
        if (super.isOverOpenMenu(mouseX, mouseY)) {
            return true;
        }
        if (this.addDropdown.isOpen() && this.addDropdown.hitTest(mouseX, mouseY) >= 0) {
            return true;
        }
        return this.grid != null && this.grid.isOverOpenMenu(mouseX, mouseY);
    }

    // region 操作

    private void openGrid(Family family) {
        RegisterComponentGridEvent.Entry entry = family.entry();
        DefinedParticleEffect effect = this.screen.effect;
        if (entry == null || effect == null) {
            this.screen.error = Component.literal("No editor grid for " + family.present().getFirst());
            return;
        }
        IComponent component = effect.components.get(family.present().getFirst());
        if (component == null) {
            this.screen.error = Component.literal("No editor grid for " + family.present().getFirst());
            return;
        }
        // 点开时才创建 Grid：列表每帧渲染不再为每个族 new 一个 Grid
        ComponentGrid probe = entry.factory().apply(this.screen, this.font);
        probe.setComponentId(family.present().getFirst());
        try {
            probe.load(component);
        } catch (RuntimeException e) {
            this.screen.error = Component.literal(String.valueOf(e.getMessage()));
            return;
        }
        this.grid = probe;
        this.selectedEntry = entry;
        this.gridScroll = 0;
        this.screen.error = null;
        this.addDropdown.close();
    }

    private void add(RegisterComponentGridEvent.Entry entry) {
        DefinedParticleEffect effect = this.screen.effect;
        if (effect == null) {
            return;
        }
        ComponentGrid created = entry.factory().apply(this.screen, this.font);
        ComponentGrid.BuiltComponent built;
        try {
            built = created.createDefault();
        } catch (RuntimeException e) {
            this.screen.error = Component.literal(String.valueOf(e.getMessage()));
            return;
        }
        Map<ResourceLocation, IComponent> components = new HashMap<>(effect.components);
        entry.variants().forEach(components::remove);
        components.put(built.id(), built.component());
        this.screen.effect = new DefinedParticleEffect(effect.description, effect.curves, components, effect.events);
        this.respawnScene();
        this.grid = created;
        this.selectedEntry = entry;
        this.gridScroll = 0;
        created.setComponentId(built.id());
        try {
            created.load(built.component());
        } catch (RuntimeException ignored) {
        }
    }

    private void applyGrid() {
        ComponentGrid active = this.grid;
        RegisterComponentGridEvent.Entry entry = this.selectedEntry;
        DefinedParticleEffect effect = this.screen.effect;
        if (active == null || effect == null) {
            return;
        }
        ComponentGrid.BuiltComponent built;
        try {
            built = active.build();
        } catch (RuntimeException e) {
            this.screen.error = Component.literal(String.valueOf(e.getMessage()));
            return;
        }
        Map<ResourceLocation, IComponent> components = new HashMap<>(effect.components);
        if (entry != null) {
            entry.variants().forEach(components.keySet()::remove);
        }
        components.put(built.id(), built.component());
        this.screen.effect = new DefinedParticleEffect(effect.description, effect.curves, components, effect.events);
        this.respawnScene();
    }

    private void remove() {
        RegisterComponentGridEvent.Entry entry = this.selectedEntry;
        DefinedParticleEffect effect = this.screen.effect;
        if (entry == null || effect == null || entry.required()) {
            return;
        }
        Map<ResourceLocation, IComponent> components = new HashMap<>(effect.components);
        entry.variants().forEach(components.keySet()::remove);
        this.screen.effect = new DefinedParticleEffect(effect.description, effect.curves, components, effect.events);
        this.closeGrid();
        this.respawnScene();
    }

    private void respawnScene() {
        this.screen.error = null;
        this.screen.manager.clearScene();
        if (this.screen.selectedParticle != null) {
            this.screen.spawnEmitter(this.screen.selectedParticle);
        }
    }

    private void closeGrid() {
        if (this.grid != null) {
            this.grid.closeEditing();
        }
        this.grid = null;
        this.selectedEntry = null;
        this.gridScroll = 0;
    }

    private List<RegisterComponentGridEvent.Entry> addCandidates() {
        DefinedParticleEffect effect = this.screen.effect;
        if (effect == null) {
            return List.of();
        }
        // 每帧渲染列表时不要反复 new Grid 去问 canAdd：按「当前效果实例」缓存，效果被替换时自然失效
        if (effect == this.candidatesFor) {
            return this.candidatesCache;
        }
        this.candidatesFor = effect;
        List<RegisterComponentGridEvent.Entry> candidates = new ArrayList<>();
        for (RegisterComponentGridEvent.Entry entry : RegisterComponentGridEvent.entries()) {
            if (isPresent(effect, entry)) {
                continue;
            }
            if (entry.factory().apply(this.screen, this.font).canAdd()) {
                candidates.add(entry);
            }
        }
        candidates.sort(Comparator.comparing(RegisterComponentGridEvent.Entry::label));
        this.candidatesCache = candidates;
        return candidates;
    }

    private static boolean isPresent(DefinedParticleEffect effect, RegisterComponentGridEvent.Entry entry) {
        for (ResourceLocation id : entry.variants()) {
            if (effect.components.containsKey(id)) {
                return true;
            }
        }
        return false;
    }

    // endregion
}

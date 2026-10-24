package org.mesdag.particlestorm.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.RegisterCustomParticleTypeEvent;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.data.description.DescriptionMaterial;
import org.mesdag.particlestorm.data.description.DescriptionParameters;
import org.mesdag.particlestorm.data.description.ParticleDescription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/// 「描述」页签：ParticleDescription 的修改表单（identifier / type / material / texture 四行）。
///
/// 行布局和组件 Grid 一致：**标签在上、值缩进在下一行**（行高 24）；点 identifier 的值行打开 EditBox，
/// type / material 的值行是下拉框，texture 只读。
///
/// 修改只影响编辑器缓存的完整效果：先更新缓存 → 清空场景 → 用新效果重新发射；
/// 发射器预设由 [EditorParticleEmitter] 从缓存重建，所以改完立刻生效、不写回引擎数据。
final class DescriptionTab extends EditorTab {
    private static final int ROW_HEIGHT = 24;
    private static final int LABEL_Y = 2;
    private static final int VALUE_Y = 13;
    private static final int VALUE_X = 12;
    private static final int LIST_ROW_HEIGHT = 12;

    /// identifier 行用的原版编辑框（是否在编辑直接看它的焦点状态）。
    private final EditBox identifierBox;
    private final DropdownWidget typeDropdown;
    private final DropdownWidget materialDropdown;

    DescriptionTab(EditorScreen screen, Font font) {
        super(screen, font);
        this.identifierBox = new EditBox(font, 2, screen.toolboxContentTop() + VALUE_Y - 2,
                Math.max(20, screen.toolboxWidth() - 4), 13, Component.literal("identifier"));
        this.identifierBox.setMaxLength(128);
        this.identifierBox.setValue("");
        this.identifierBox.setFocused(false);
        int menuWidth = Math.max(20, screen.toolboxWidth() - 4);
        this.typeDropdown = new DropdownWidget(2, screen.toolboxContentTop() + 2 * ROW_HEIGHT, menuWidth, LIST_ROW_HEIGHT);
        this.materialDropdown = new DropdownWidget(2, screen.toolboxContentTop() + 3 * ROW_HEIGHT, menuWidth, LIST_ROW_HEIGHT);
    }

    @Override
    Component title() {
        return Component.translatable("particle_editor.tab.description");
    }

    @Override
    void render(GuiGraphics guiGraphics, int contentTop, int mouseX, int mouseY) {
        DefinedParticleEffect effect = this.screen.effect;
        if (effect == null) {
            guiGraphics.drawString(this.font, Component.translatable("particle_editor.error.no_particle"), 6, contentTop + 2, 0x888888, false);
            return;
        }
        List<ParticleType<?>> types = typeValues();
        DescriptionMaterial[] materials = DescriptionMaterial.values();
        // 窗口缩放后页签位置会变：每帧按当前工具框宽度重新摆放
        this.typeDropdown.setPosition(2, this.screen.toolboxContentTop() + 2 * ROW_HEIGHT);
        this.materialDropdown.setPosition(2, this.screen.toolboxContentTop() + 3 * ROW_HEIGHT);
        this.typeDropdown.setItems(types.stream().map(DescriptionTab::typeName).toList());
        this.typeDropdown.setSelectedIndex(types.indexOf(effect.description.type()));
        this.materialDropdown.setItems(Arrays.stream(materials).map(DescriptionTab::materialLabel).toList());
        this.materialDropdown.setSelectedIndex(materialIndex(materials, effect.description.parameters().material()));

        String[] labels = Arrays.stream(new String[]{"identifier", "type", "material", "texture"})
                .map(field -> Component.translatable("particle_editor.description." + field).getString())
                .toArray(String[]::new);
        String[] values = {
                effect.description.identifier().toString(),
                typeName(effect.description.type()),
                materialLabel(effect.description.parameters().material()),
                effect.description.parameters().texture().toString()
        };
        int width = this.screen.toolboxWidth();
        int valueWidth = Math.max(10, width - 10 - VALUE_X);
        for (int i = 0; i < labels.length; i++) {
            int rowTop = contentTop + i * ROW_HEIGHT;
            boolean identifier = i == 0;
            boolean type = i == 1;
            boolean material = i == 2;
            boolean texture = i == 3;
            boolean editingThisRow = identifier && this.identifierBox.isFocused();
            boolean dropdownRow = (type && this.typeDropdown.isOpen()) || (material && this.materialDropdown.isOpen());
            boolean rowHover = mouseX >= 2 && mouseX < width - 4 && mouseY >= rowTop && mouseY < rowTop + ROW_HEIGHT
                    && !this.isOverOpenMenu(mouseX, mouseY);
            if (rowHover && !editingThisRow && !dropdownRow) {
                guiGraphics.fill(2, rowTop, width - 4, rowTop + ROW_HEIGHT, 0x20FFFFFF);
            }
            guiGraphics.drawString(this.font, labels[i], 6, rowTop + LABEL_Y, 0x999999, false);
            if (editingThisRow) {
                // 正在编辑 identifier：值行交给原版 EditBox 绘制（在下方统一渲染）
                continue;
            }
            guiGraphics.drawString(this.font, this.screen.truncatedFor(values[i], valueWidth, rowHover), VALUE_X, rowTop + VALUE_Y,
                    identifier || type || material || texture ? 0xFFFFFF : 0xCCCCCC, false);
            if (type || material || texture) {
                this.screen.renderDropdownArrow(guiGraphics, width - 13, rowTop + VALUE_Y + 2);
            }
        }
        if (this.identifierBox.isFocused()) {
            this.identifierBox.render(guiGraphics, mouseX, mouseY, 0);
        }
        this.typeDropdown.render(guiGraphics, this.font, mouseX, mouseY, this.screen.height);
        this.tooltipForMenu(this.typeDropdown, mouseX, mouseY);
        this.materialDropdown.render(guiGraphics, this.font, mouseX, mouseY, this.screen.height);
        this.tooltipForMenu(this.materialDropdown, mouseX, mouseY);
    }

    @Override
    boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.identifierBox.isFocused() && this.identifierBox.isMouseOver(mouseX, mouseY)) {
            // 点编辑框本身：交给 EditBox 处理光标定位
            this.identifierBox.mouseClicked(mouseX, mouseY, 0);
            return true;
        }
        // 展开的下拉列表画在行上面（可能盖住下面的行），先处理列表的点击
        if (this.typeDropdown.isOpen()) {
            int index = this.typeDropdown.hitTest(mouseX, mouseY);
            this.typeDropdown.close();
            List<ParticleType<?>> types = typeValues();
            if (index >= 0 && index < types.size()) {
                this.applyType(types.get(index));
            }
            return true;
        }
        if (this.materialDropdown.isOpen()) {
            int index = this.materialDropdown.hitTest(mouseX, mouseY);
            this.materialDropdown.close();
            DescriptionMaterial[] materials = DescriptionMaterial.values();
            if (index >= 0 && index < materials.length) {
                this.applyMaterial(materials[index]);
            }
            return true;
        }
        if (isRow(mouseX, mouseY, 0)) {
            // identifier：打开原版 EditBox（占满整行）
            this.closeEditing();
            this.openIdentifierEditor();
            return true;
        }
        if (isRow(mouseX, mouseY, 1)) {
            this.identifierBox.setFocused(false);
            this.screen.toggleMenu(this.typeDropdown);
            return true;
        }
        if (isRow(mouseX, mouseY, 2)) {
            this.identifierBox.setFocused(false);
            this.screen.toggleMenu(this.materialDropdown);
            return true;
        }
        if (isRow(mouseX, mouseY, 3)) {
            // texture：按当前材质打开图集查看器，选中的 sprite 直接绑定
            this.identifierBox.setFocused(false);
            this.closeEditing();
            DefinedParticleEffect current = this.screen.effect;
            if (current != null) {
                DescriptionParameters parameters = current.description.parameters();
                this.screen.openAtlasViewer(parameters.material(), parameters.texture(), this::applyTexture);
            }
            return true;
        }
        return false;
    }

    @Override
    boolean isCapturingInput() {
        return this.identifierBox.isFocused() || this.typeDropdown.isOpen() || this.materialDropdown.isOpen();
    }

    @Override
    boolean scroll(double mouseX, double mouseY, double scrollY) {
        if (this.typeDropdown.contains(mouseX, mouseY)) {
            this.typeDropdown.scroll(scrollY);
            return true;
        }
        if (this.materialDropdown.contains(mouseX, mouseY)) {
            this.materialDropdown.scroll(scrollY);
            return true;
        }
        return false;
    }

    @Override
    void closeEditing() {
        this.identifierBox.setFocused(false);
        this.typeDropdown.close();
        this.materialDropdown.close();
    }

    @Override
    boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.identifierBox.isFocused()) {
            if (keyCode == 256) { // ESC：取消编辑
                this.identifierBox.setFocused(false);
                return true;
            }
            if (keyCode == 257) { // ENTER：提交
                this.applyIdentifierEdit();
                return true;
            }
            return this.identifierBox.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == 256 && (this.typeDropdown.isOpen() || this.materialDropdown.isOpen())) {
            this.closeEditing();
            return true;
        }
        return false;
    }

    @Override
    boolean charTyped(char codePoint, int modifiers) {
        if (this.identifierBox.isFocused()) {
            return this.identifierBox.charTyped(codePoint, modifiers);
        }
        return false;
    }

    @Override
    protected boolean isOverOpenMenu(double mouseX, double mouseY) {
        return super.isOverOpenMenu(mouseX, mouseY)
                || (this.typeDropdown.isOpen() && this.typeDropdown.hitTest(mouseX, mouseY) >= 0)
                || (this.materialDropdown.isOpen() && this.materialDropdown.hitTest(mouseX, mouseY) >= 0);
    }

    /// 打开原版 EditBox 编辑 identifier：盖在 identifier 的值行上。
    private void openIdentifierEditor() {
        DefinedParticleEffect current = this.screen.effect;
        if (current == null) {
            return;
        }
        this.identifierBox.setX(2);
        this.identifierBox.setY(this.screen.toolboxContentTop() + VALUE_Y - 2);
        this.identifierBox.setWidth(Math.max(20, this.screen.toolboxWidth() - 4));
        this.identifierBox.setValue(current.description.identifier().toString());
        this.identifierBox.setFocused(true);
    }

    /// 提交 identifier 编辑：先更新缓存，再清场景并重新发射（新 id 只在编辑器内使用）。
    private void applyIdentifierEdit() {
        this.identifierBox.setFocused(false);
        DefinedParticleEffect current = this.screen.effect;
        String value = this.identifierBox.getValue().trim();
        ResourceLocation identifier = ResourceLocation.tryParse(value);
        if (identifier == null) {
            this.screen.error = Component.translatable("particle_editor.error.unknown_identifier", value);
            return;
        }
        if (current == null) {
            return;
        }
        this.screen.effect = new DefinedParticleEffect(
                new ParticleDescription(identifier, current.description.parameters(), current.description.type()),
                current.curves, current.components, current.events);
        this.afterEffectChanged(identifier);
    }

    /// 把选中的粒子类型写进编辑器缓存，然后清场景并重新发射。
    private void applyType(ParticleType<?> type) {
        DefinedParticleEffect current = this.screen.effect;
        this.typeDropdown.close();
        if (current == null) {
            return;
        }
        this.screen.effect = new DefinedParticleEffect(
                new ParticleDescription(current.description.identifier(), current.description.parameters(), type),
                current.curves, current.components, current.events);
        this.afterEffectChanged(this.screen.selectedParticle);
    }

    /// 把选中的材质写进编辑器缓存（贴图不变，只更新材质）。
    private void applyMaterial(DescriptionMaterial material) {
        DefinedParticleEffect current = this.screen.effect;
        if (current == null) {
            return;
        }
        DescriptionParameters oldParameters = current.description.parameters();
        DescriptionParameters newParameters = new DescriptionParameters(material, oldParameters.texture());
        // 材质换了 → 图集也换了：按贴图名在新图集里找到 sprite 并登记进共享集合
        registerTexture(material, newParameters);
        this.screen.effect = new DefinedParticleEffect(
                new ParticleDescription(current.description.identifier(), newParameters, current.description.type()),
                current.curves, current.components, current.events);
        this.materialDropdown.close();
        this.afterEffectChanged(this.screen.selectedParticle);
    }

    /// 绑定图集查看器里选中的 sprite：贴图名写回描述参数，并把 sprite 登记进共享集合（渲染时按名字查）。
    private void applyTexture(TextureAtlasSprite sprite) {
        DefinedParticleEffect current = this.screen.effect;
        if (current == null) {
            return;
        }
        RegisterCustomParticleTypeEvent.sprites().addSprite(sprite);
        DescriptionParameters oldParameters = current.description.parameters();
        DescriptionParameters newParameters = new DescriptionParameters(oldParameters.material(), sprite.contents().name());
        this.screen.effect = new DefinedParticleEffect(
                new ParticleDescription(current.description.identifier(), newParameters, current.description.type()),
                current.curves, current.components, current.events);
        this.afterEffectChanged(this.screen.selectedParticle);
    }

    /// 按贴图名在当前材质的图集里找到 sprite 并登记进共享集合（找不到就留给 missing）。
    private static void registerTexture(DescriptionMaterial material, DescriptionParameters parameters) {
        TextureAtlasSprite sprite = AtlasViewer.spriteFor(material, parameters.texture());
        if (sprite != null) {
            RegisterCustomParticleTypeEvent.sprites().addSprite(sprite);
        }
    }

    /// 效果缓存更新后的统一收尾：清空场景，再用新效果重新发射。
    private void afterEffectChanged(@Nullable ResourceLocation particle) {
        this.screen.error = null;
        this.screen.manager.clearScene();
        if (particle != null) {
            this.screen.spawnEmitter(particle);
        }
    }

    private boolean isRow(double mouseX, double mouseY, int index) {
        int rowTop = this.screen.toolboxContentTop() + index * ROW_HEIGHT;
        return mouseX >= 0 && mouseX < this.screen.toolboxWidth() && mouseY >= rowTop && mouseY < rowTop + ROW_HEIGHT;
    }

    private static List<ParticleType<?>> typeValues() {
        List<ParticleType<?>> types = new ArrayList<>(RegisterCustomParticleTypeEvent.map().keySet());
        types.sort(Comparator.comparing(DescriptionTab::typeName));
        return types;
    }

    private static String typeName(ParticleType<?> type) {
        ResourceLocation id = BuiltInRegistries.PARTICLE_TYPE.getKey(type);
        return id == null ? String.valueOf(type) : id.toString();
    }

    /// 材质下拉/值行的显示名：`particle_editor.minecraft.material.<serialized_name>`
    /// （自定义材质没有翻译时原样显示序列化名）。
    private static String materialLabel(DescriptionMaterial material) {
        String name = material.getSerializedName();
        return Component.translatable("particle_editor.minecraft.material." + name).getString();
    }

    private static int materialIndex(DescriptionMaterial[] materials, DescriptionMaterial material) {
        for (int i = 0; i < materials.length; i++) {
            if (materials[i] == material) {
                return i;
            }
        }
        return -1;
    }
}

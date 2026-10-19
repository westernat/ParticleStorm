package org.mesdag.particlestorm.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.mesdag.particlestorm.api.IComponent;
import org.mesdag.particlestorm.data.ConditionDispatch;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.data.component.*;
import org.mesdag.particlestorm.data.molang.BoolMolangExp;
import org.mesdag.particlestorm.data.molang.FloatMolangExp3;

import java.util.*;
import java.util.function.IntConsumer;

/// 事件 / 碰撞 / 多形状组件的编辑 Grid（原先都是只读的 `GridReadOnly`）。
///
/// 事件引用统一用**添加下拉**：点「＋ …」按钮展开当前效果里定义的可用事件（`effect.events` 的键），
/// 选中后才会把该事件名加到条目里；每个条目下面都有 `×` 删除该条（不用「只删最后一个」）。
final class EventGridSupport {
    private EventGridSupport() {
    }

    static List<String> splitEvents(String text) {
        return Arrays.stream(text.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toList();
    }

    static String joinEvents(List<String> events) {
        return String.join(", ", events);
    }

    static BoolMolangExp boolOf(String text) {
        String value = text.trim();
        if (value.isEmpty() || value.equalsIgnoreCase("true")) {
            return BoolMolangExp.TRUE;
        }
        if (value.equalsIgnoreCase("false")) {
            return BoolMolangExp.FALSE;
        }
        return BoolMolangExp.ofExpression(value);
    }

    static String boolText(BoolMolangExp exp) {
        return exp.getExpStr().isEmpty() ? String.valueOf(exp.getConstant()) : exp.getExpStr();
    }

    static String localized(String group, String key) {
        return Component.translatable("particle_editor.minecraft." + group + "." + key).getString();
    }

    static String nextTime(List<String[]> entries) {
        if (entries.isEmpty()) {
            return "0.0";
        }
        try {
            return Float.toString(Float.parseFloat(entries.getLast()[0].trim()) + 1.0F);
        } catch (NumberFormatException e) {
            return "0.0";
        }
    }
}

/// 事件列表 Grid 的基类：在「＋ 添加事件」按钮下方展开**可用事件下拉**（当前效果里定义过的事件），
/// 选中后回调 `onPick(index)`；下拉的渲染、点击、滚轮、tooltip 都由这里统一处理。
abstract class EventListGrid extends ComponentGrid {
    private static final int LIST_ROW_HEIGHT = 12;

    private final DropdownWidget addDropdown;
    private List<String> addItems = List.of();
    private IntConsumer addHandler = index -> {
    };
    EventListGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.addDropdown = new DropdownWidget(2, 0, Math.max(20, screen.toolboxWidth() - 4), LIST_ROW_HEIGHT);
    }

    /// 当前效果里定义的可用事件名（按字母序）。
    protected List<String> availableEvents() {
        DefinedParticleEffect effect = this.screen.effect;
        if (effect == null) {
            return List.of();
        }
        return effect.events.keySet().stream().sorted().toList();
    }

    /// 打开添加下拉：`buttonTop` 是「＋」按钮行的 y，列表展开在按钮行下方。
    ///
    /// 没有任何可用事件时，下拉里显示一条 `No events available` 占位项（点了不做事），而不是常驻错误提示。
    protected void openAddEventDropdown(int buttonTop, IntConsumer onPick) {
        this.addItems = this.availableEvents();
        this.addHandler = onPick;
        List<String> shown = this.addItems.isEmpty()
                ? List.of(Component.translatable("particle_editor.events.none_available").getString())
                : this.addItems;
        this.addDropdown.setItems(shown);
        this.addDropdown.setPosition(2, buttonTop + ROW_HEIGHT);
        this.addDropdown.toggle();
    }

    /// 当前添加下拉里第 `index` 个事件名（选中后回调里用）。
    protected String addItem(int index) {
        return index >= 0 && index < this.addItems.size() ? this.addItems.get(index) : "";
    }

    /// 「＋ 添加事件」按钮：label 由子类给，点击展开下拉。
    protected void addEventButton(String label, IntConsumer onPick) {
        this.button(label, top -> this.openAddEventDropdown(top, onPick));
    }

    /// 一组「事件引用」行：每条一个可编辑事件名 + 独立删除按钮，末尾一个「＋」从可用事件下拉里追加。
    protected void addEventRefRows(String group, String field, List<String> entries) {
        for (int i = 0; i < entries.size(); i++) {
            int index = i;
            String label = EventGridSupport.localized(group, field) + "[" + (index + 1) + "]";
            this.text(() -> label, entries.get(index), text -> {
                entries.set(index, text.trim());
                this.markChanged();
            });
            this.button("× " + EventGridSupport.localized(group, field + "_remove"), top -> {
                entries.remove(index);
                this.rebuildRows();
                this.markChanged();
            });
        }
        this.addEventButton("+ " + EventGridSupport.localized(group, field + "_add"), pick -> {
            entries.add(this.addItem(pick));
            this.rebuildRows();
            this.markChanged();
        });
    }

    /// 由子类重建全部行（增删条目后调用）。
    protected abstract void rebuildRows();

    @Override
    void render(GuiGraphics guiGraphics, int top, int mouseX, int mouseY) {
        super.render(guiGraphics, top, mouseX, mouseY);
        this.addDropdown.render(guiGraphics, this.font, mouseX, mouseY, this.screen.height);
        if (this.addDropdown.isOpen()) {
            int index = this.addDropdown.hitTest(mouseX, mouseY);
            String item = this.addDropdown.item(index);
            if (item != null && this.font.width(item) > this.addDropdown.textWidth()) {
                this.screen.hoveredTooltip = Component.literal(item);
            }
        }
    }

    @Override
    boolean mouseClicked(double mouseX, double mouseY) {
        if (this.addDropdown.isOpen()) {
            int index = this.addDropdown.hitTest(mouseX, mouseY);
            this.addDropdown.close();
            if (index >= 0 && index < this.addItems.size()) {
                this.addHandler.accept(index);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY);
    }

    @Override
    boolean isCapturingInput() {
        return this.addDropdown.isOpen() || super.isCapturingInput();
    }

    @Override
    boolean hasOpenMenus() {
        return this.addDropdown.isOpen() || super.hasOpenMenus();
    }

    @Override
    boolean isOverOpenMenu(double mouseX, double mouseY) {
        return (this.addDropdown.isOpen() && this.addDropdown.hitTest(mouseX, mouseY) >= 0) || super.isOverOpenMenu(mouseX, mouseY);
    }

    @Override
    boolean scroll(double mouseX, double mouseY, double scrollY) {
        if (this.addDropdown.contains(mouseX, mouseY)) {
            this.addDropdown.scroll(scrollY);
            return true;
        }
        return super.scroll(mouseX, mouseY, scrollY);
    }

    @Override
    void closeEditing() {
        this.addDropdown.close();
        super.closeEditing();
    }
}

/// `particle_lifetime_events`：创建事件、过期事件（每条一个事件引用）+ timeline（时间 → 事件引用）。
final class ParticleLifeTimeEventsGrid extends EventListGrid {
    private final List<String> creation = new ArrayList<>();
    private final List<String> expiration = new ArrayList<>();
    /// `[时间, 事件(逗号分隔)]`
    private final List<String[]> timeline = new ArrayList<>();

    ParticleLifeTimeEventsGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.setComponentId(ParticleLifeTimeEvents.ID);
        this.rebuildRows();
    }

    @Override
    protected void rebuildRows() {
        this.resetRows();
        this.addEventRefRows("particle_lifetime_events", "creation_event", this.creation);
        this.addEventRefRows("particle_lifetime_events", "expiration_event", this.expiration);
        for (int i = 0; i < this.timeline.size(); i++) {
            int index = i;
            this.vector(() -> "timeline[" + this.timeline.get(index)[0] + "]", 2, this.timeline.get(index), parts -> {
                this.timeline.set(index, parts);
                this.markChanged();
            });
            this.button("× " + EventGridSupport.localized("particle_lifetime_events", "timeline_remove"), top -> {
                this.timeline.remove(index);
                this.rebuildRows();
                this.markChanged();
            });
        }
        this.addEventButton("+ " + EventGridSupport.localized("particle_lifetime_events", "timeline_add"), pick -> {
            this.timeline.add(new String[]{EventGridSupport.nextTime(this.timeline), this.addItem(pick)});
            this.rebuildRows();
            this.markChanged();
        });
    }

    @Override
    public BuiltComponent createDefault() {
        return new BuiltComponent(ParticleLifeTimeEvents.ID, new ParticleLifeTimeEvents(List.of(), List.of(), Map.of()));
    }

    @Override
    public BuiltComponent build() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (String[] entry : this.timeline) {
            map.put(entry[0].trim(), EventGridSupport.splitEvents(entry[1]));
        }
        return new BuiltComponent(ParticleLifeTimeEvents.ID, new ParticleLifeTimeEvents(
                List.copyOf(this.creation), List.copyOf(this.expiration), map));
    }

    @Override
    public void load(IComponent component) {
        ParticleLifeTimeEvents events = (ParticleLifeTimeEvents) component;
        this.creation.clear();
        this.creation.addAll(events.creationEvent);
        this.expiration.clear();
        this.expiration.addAll(events.expirationEvent);
        this.timeline.clear();
        events.timeline.forEach((time, list) -> this.timeline.add(new String[]{time, EventGridSupport.joinEvents(list)}));
        this.rebuildRows();
    }
}

/// `emitter_lifetime_events`：创建 / 过期事件（每条一个事件引用）+ timeline + 移动距离事件 + 循环移动距离事件。
final class EmitterLifetimeEventsGrid extends EventListGrid {
    private final List<String> creation = new ArrayList<>();
    private final List<String> expiration = new ArrayList<>();
    private final List<String[]> timeline = new ArrayList<>();
    private final List<String[]> travel = new ArrayList<>();
    private final List<String[]> looping = new ArrayList<>();

    EmitterLifetimeEventsGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.setComponentId(EmitterLifetimeEvents.ID);
        this.rebuildRows();
    }

    @Override
    protected void rebuildRows() {
        this.resetRows();
        this.addEventRefRows("emitter_lifetime_events", "creation_event", this.creation);
        this.addEventRefRows("emitter_lifetime_events", "expiration_event", this.expiration);
        for (int i = 0; i < this.timeline.size(); i++) {
            int index = i;
            this.vector(() -> "timeline[" + this.timeline.get(index)[0] + "]", 2, this.timeline.get(index), parts -> {
                this.timeline.set(index, parts);
                this.markChanged();
            });
            this.button("× " + EventGridSupport.localized("emitter_lifetime_events", "timeline_remove"), top -> {
                this.timeline.remove(index);
                this.rebuildRows();
                this.markChanged();
            });
        }
        this.addEventButton("+ " + EventGridSupport.localized("emitter_lifetime_events", "timeline_add"), pick -> {
            this.timeline.add(new String[]{EventGridSupport.nextTime(this.timeline), this.addItem(pick)});
            this.rebuildRows();
            this.markChanged();
        });
        for (int i = 0; i < this.travel.size(); i++) {
            int index = i;
            this.vector(() -> "travel_distance[" + this.travel.get(index)[0] + "]", 2, this.travel.get(index), parts -> {
                this.travel.set(index, parts);
                this.markChanged();
            });
            this.button("× " + EventGridSupport.localized("emitter_lifetime_events", "travel_remove"), top -> {
                this.travel.remove(index);
                this.rebuildRows();
                this.markChanged();
            });
        }
        this.addEventButton("+ " + EventGridSupport.localized("emitter_lifetime_events", "travel_add"), pick -> {
            this.travel.add(new String[]{EventGridSupport.nextTime(this.travel), this.addItem(pick)});
            this.rebuildRows();
            this.markChanged();
        });
        for (int i = 0; i < this.looping.size(); i++) {
            int index = i;
            this.vector(() -> "looping_travel_distance[" + this.looping.get(index)[0] + "]", 2, this.looping.get(index), parts -> {
                this.looping.set(index, parts);
                this.markChanged();
            });
            this.button("× " + EventGridSupport.localized("emitter_lifetime_events", "looping_remove"), top -> {
                this.looping.remove(index);
                this.rebuildRows();
                this.markChanged();
            });
        }
        this.addEventButton("+ " + EventGridSupport.localized("emitter_lifetime_events", "looping_add"), pick -> {
            this.looping.add(new String[]{EventGridSupport.nextTime(this.looping), this.addItem(pick)});
            this.rebuildRows();
            this.markChanged();
        });
    }

    @Override
    public BuiltComponent createDefault() {
        return new BuiltComponent(EmitterLifetimeEvents.ID,
                new EmitterLifetimeEvents(List.of(), List.of(), Map.of(), Map.of(), List.of()));
    }

    @Override
    public BuiltComponent build() {
        Map<String, List<String>> timelineMap = new LinkedHashMap<>();
        for (String[] entry : this.timeline) {
            timelineMap.put(entry[0].trim(), EventGridSupport.splitEvents(entry[1]));
        }
        Map<String, List<String>> travelMap = new LinkedHashMap<>();
        for (String[] entry : this.travel) {
            travelMap.put(entry[0].trim(), EventGridSupport.splitEvents(entry[1]));
        }
        List<EmitterLifetimeEvents.LoopingTravelDistanceEvent> loops = new ArrayList<>();
        for (String[] entry : this.looping) {
            loops.add(new EmitterLifetimeEvents.LoopingTravelDistanceEvent(
                    Float.parseFloat(entry[0].trim()), EventGridSupport.splitEvents(entry[1])));
        }
        return new BuiltComponent(EmitterLifetimeEvents.ID, new EmitterLifetimeEvents(
                List.copyOf(this.creation), List.copyOf(this.expiration),
                timelineMap, travelMap, loops));
    }

    @Override
    public void load(IComponent component) {
        EmitterLifetimeEvents events = (EmitterLifetimeEvents) component;
        this.creation.clear();
        this.creation.addAll(events.creationEvent);
        this.expiration.clear();
        this.expiration.addAll(events.expirationEvent);
        this.timeline.clear();
        events.timeline.forEach((time, list) -> this.timeline.add(new String[]{time, EventGridSupport.joinEvents(list)}));
        this.travel.clear();
        events.travelDistanceEvents.forEach((time, list) -> this.travel.add(new String[]{time, EventGridSupport.joinEvents(list)}));
        this.looping.clear();
        for (EmitterLifetimeEvents.LoopingTravelDistanceEvent loop : events.loopingTravelDistanceEvents) {
            this.looping.add(new String[]{String.valueOf(loop.distance()), EventGridSupport.joinEvents(loop.effects())});
        }
        this.rebuildRows();
    }
}

/// `particle_motion_collision`：碰撞开关（bool 或 molang 表达式）、拖拽、恢复系数、碰撞半径、接触过期、接触事件。
final class ParticleMotionCollisionGrid extends EventListGrid {
    private String enabled = "true";
    private String collisionDrag = "0";
    private String restitution = "0";
    private String radius = "0";
    private boolean expireOnContact;
    private final List<String[]> events = new ArrayList<>();

    ParticleMotionCollisionGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.setComponentId(ParticleMotionCollision.ID);
        this.rebuildRows();
    }

    @Override
    protected void rebuildRows() {
        this.resetRows();
        this.text("enabled", this.enabled, text -> {
            this.enabled = text;
            this.markChanged();
        });
        this.text("collision_drag", this.collisionDrag, text -> {
            this.collisionDrag = text;
            this.markChanged();
        });
        this.text("coefficient_of_restitution", this.restitution, text -> {
            this.restitution = text;
            this.markChanged();
        });
        this.text("collision_radius", this.radius, text -> {
            this.radius = text;
            this.markChanged();
        });
        this.toggle("expire_on_contact", this.expireOnContact, value -> {
            this.expireOnContact = value;
            this.markChanged();
        });
        for (int i = 0; i < this.events.size(); i++) {
            int index = i;
            this.vector(() -> "events[" + (index + 1) + "]", 2, this.events.get(index), parts -> {
                this.events.set(index, parts);
                this.markChanged();
            });
            this.button("× " + EventGridSupport.localized("particle_motion_collision", "event_remove"), top -> {
                this.events.remove(index);
                this.rebuildRows();
                this.markChanged();
            });
        }
        this.addEventButton("+ " + EventGridSupport.localized("particle_motion_collision", "event_add"), pick -> {
            this.events.add(new String[]{this.addItem(pick), "2.0"});
            this.rebuildRows();
            this.markChanged();
        });
    }

    @Override
    public BuiltComponent createDefault() {
        return new BuiltComponent(ParticleMotionCollision.ID,
                new ParticleMotionCollision(BoolMolangExp.TRUE, 0.0F, 0.0F, 0.0F, false, List.of()));
    }

    @Override
    public BuiltComponent build() {
        List<ParticleMotionCollision.Event> list = new ArrayList<>();
        for (String[] entry : this.events) {
            list.add(new ParticleMotionCollision.Event(entry[0].trim(), Float.parseFloat(entry[1].trim())));
        }
        return new BuiltComponent(ParticleMotionCollision.ID, new ParticleMotionCollision(
                EventGridSupport.boolOf(this.enabled),
                Float.parseFloat(this.collisionDrag.trim()),
                Float.parseFloat(this.restitution.trim()),
                Float.parseFloat(this.radius.trim()),
                this.expireOnContact,
                list));
    }

    @Override
    public void load(IComponent component) {
        ParticleMotionCollision collision = (ParticleMotionCollision) component;
        this.enabled = EventGridSupport.boolText(collision.enabled());
        this.collisionDrag = String.valueOf(collision.collisionDrag());
        this.restitution = String.valueOf(collision.coefficientOfRestitution());
        this.radius = String.valueOf(collision.collisionRadius());
        this.expireOnContact = collision.expireOnContact();
        this.events.clear();
        for (ParticleMotionCollision.Event event : collision.events()) {
            this.events.add(new String[]{event.event(), String.valueOf(event.minSpeed())});
        }
        this.rebuildRows();
    }
}

/// `emitter_multi_shape`：按条件触发的多个发射器形状。每个条目 = 条件表达式 + 一套完整的形状字段
/// （和单独的 `EmitterShapeGrid` 共用 [ShapeFields]），下方按钮增删条目。
final class EmitterMultiShapeGrid extends ComponentGrid {
    private final List<Entry> entries = new ArrayList<>();

    private static final class Entry {
        String condition = "true";
        ShapeFields shape;
    }

    EmitterMultiShapeGrid(EditorScreen screen, Font font) {
        super(screen, font);
        this.setComponentId(EmitterMultiShape.ID);
        this.entries.add(this.newEntry());
        this.rebuildRows();
    }

    private Entry newEntry() {
        Entry entry = new Entry();
        entry.shape = new ShapeFields(this::rebuildRows, () -> "shape[" + this.entries.indexOf(entry) + "].");
        return entry;
    }

    private void rebuildRows() {
        this.resetRows();
        for (Entry entry : this.entries) {
            String prefix = "shape[" + this.entries.indexOf(entry) + "]";
            this.text(() -> prefix + ".condition", entry.condition, text -> {
                entry.condition = text;
                this.markChanged();
            });
            entry.shape.addRows(this);
        }
        this.button("+ " + EventGridSupport.localized("emitter_multi_shape", "shape_add"), () -> {
            this.entries.add(this.newEntry());
            this.rebuildRows();
            this.markChanged();
        });
        if (this.entries.size() > 1) {
            this.button("- " + EventGridSupport.localized("emitter_multi_shape", "shape_remove"), () -> {
                this.entries.removeLast();
                this.rebuildRows();
                this.markChanged();
            });
        }
    }

    @Override
    public BuiltComponent createDefault() {
        return new BuiltComponent(EmitterMultiShape.ID, new EmitterMultiShape(List.of(
                new ConditionDispatch<>(BoolMolangExp.TRUE,
                        new EmitterShape.Point(FloatMolangExp3.ZERO, EmitterShape.Direction.OUTWARDS)))));
    }

    @Override
    public BuiltComponent build() {
        List<ConditionDispatch<EmitterShape>> shapes = new ArrayList<>();
        for (Entry entry : this.entries) {
            shapes.add(new ConditionDispatch<>(EventGridSupport.boolOf(entry.condition), entry.shape.build()));
        }
        return new BuiltComponent(EmitterMultiShape.ID, new EmitterMultiShape(shapes));
    }

    @Override
    public void load(IComponent component) {
        EmitterMultiShape multi = (EmitterMultiShape) component;
        this.entries.clear();
        for (ConditionDispatch<EmitterShape> dispatch : multi.shapes()) {
            Entry entry = this.newEntry();
            entry.condition = EventGridSupport.boolText(dispatch.condition());
            entry.shape.load(dispatch.value());
            this.entries.add(entry);
        }
        if (this.entries.isEmpty()) {
            this.entries.add(this.newEntry());
        }
        this.rebuildRows();
    }
}

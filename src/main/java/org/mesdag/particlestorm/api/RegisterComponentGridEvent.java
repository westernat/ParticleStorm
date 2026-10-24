package org.mesdag.particlestorm.api;

import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.editor.ComponentGrid;
import org.mesdag.particlestorm.editor.ComponentGrids;
import org.mesdag.particlestorm.editor.EditorScreen;

import java.util.*;
import java.util.function.BiFunction;

/// 注册「组件编辑 Grid」：编辑器组件页签按**组件族**列出效果里的组件，
/// 选中后用对应 Grid 编辑；同族但有多个子类型的组件（如 `EmitterShape` 的 5 种、`EmitterRate` 的 3 种）
/// 只写一个 Grid，页签里的「类型」下拉决定具体子类型。
///
/// 内置组件的 Grid 在 [ComponentGrids#registerBuiltins] 里注册；其它模组可以在这里给自定义组件补 Grid。
public class RegisterComponentGridEvent extends Event {
    /// 一个组件族的注册信息。
    ///
    /// @param defaultId 默认（首选）子类型 id，必须包含在 `variants` 里
    /// @param label     列表里显示的族名，例如 `emitter_shape`
    /// @param variants  同族全部子类型 id（单选，下拉切换）；单 id 组件只有一个元素
    /// @param required  是否必须存在（页签里不可删除）
    /// @param factory   Grid 工厂
    public record Entry(ResourceLocation defaultId, String label, List<ResourceLocation> variants, boolean required,
                        BiFunction<EditorScreen, Font, ? extends ComponentGrid> factory) {
    }

    private static final Map<ResourceLocation, Entry> BY_ID = new LinkedHashMap<>();
    private static List<Entry> ENTRIES = List.of();

    public void register(Entry entry) {
        for (ResourceLocation id : entry.variants()) {
            BY_ID.putIfAbsent(id, entry);
        }
    }

    public void register(ResourceLocation defaultId, String label, List<ResourceLocation> variants, boolean required,
                         BiFunction<EditorScreen, Font, ? extends ComponentGrid> factory) {
        register(new Entry(defaultId, label, List.copyOf(variants), required, factory));
    }

    /// 单 id 组件的简写：不可重复、非必需、族名取 id 的 path。
    public void register(ResourceLocation id, BiFunction<EditorScreen, Font, ? extends ComponentGrid> factory) {
        register(id, id.getPath(), List.of(id), false, factory);
    }

    /// 注册内置 Grid，再让各模组补自己的；客户端启动时调用一次。
    public static void postEvent() {
        BY_ID.clear();
        RegisterComponentGridEvent event = new RegisterComponentGridEvent();
        ComponentGrids.registerBuiltins(event);
        MinecraftForge.EVENT_BUS.post(event);
        List<Entry> entries = new ArrayList<>();
        for (Entry entry : BY_ID.values()) {
            if (!entries.contains(entry)) {
                entries.add(entry);
            }
        }
        ENTRIES = List.copyOf(entries);
    }

    /// 某个组件 id 属于哪个族（未注册 Grid 的组件返回 null）。
    public static @Nullable Entry entryFor(ResourceLocation id) {
        return BY_ID.get(id);
    }

    /// 全部已注册的组件族。
    public static List<Entry> entries() {
        return Collections.unmodifiableList(ENTRIES);
    }
}

package org.mesdag.particlestorm.api;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.data.description.DescriptionMaterial;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/// 注册自定义材质的事件（在 [DescriptionMaterial#values()] 首次调用时触发）。
///
/// 材质可以带一个 `Supplier<TextureAtlas>` 声明它用哪张图集；注册图集的字段是懒取的，
/// 所以这个 supplier 只会在客户端真正取图集时执行。
public class RegisterCustomMaterialEvent extends Event implements IModBusEvent {
    private final BiFunction<String, Supplier<TextureAtlas>, DescriptionMaterial> consumer;

    public RegisterCustomMaterialEvent(BiFunction<String, Supplier<TextureAtlas>, DescriptionMaterial> function) {
        this.consumer = function;
    }

    /// 注册材质（默认使用粒子图集）。
    public DescriptionMaterial register(String name) {
        return this.register(name, null);
    }

    /// 注册材质，并声明它使用的纹理图集；`atlas` 为 null 时用粒子图集。
    public DescriptionMaterial register(String name, @Nullable Supplier<TextureAtlas> atlas) {
        return this.consumer.apply(name, atlas);
    }
}

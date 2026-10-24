package org.mesdag.particlestorm.data.description;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.fml.ModLoader;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.RegisterCustomMaterialEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/// 材质（`description.basic_render_parameters.material`）。
///
/// 每个材质带一个 `Supplier<TextureAtlas>`：这个材质用哪张纹理图集（注册时给，懒取）。
/// 默认是粒子图集（`textures/atlas/particles.png`），`terrain_sheet` 是方块图集；
/// 自定义材质想用别的图集就在 [RegisterCustomMaterialEvent] 里传自己的 supplier。
public final class DescriptionMaterial implements StringRepresentable {
    private static List<DescriptionMaterial> list = new ArrayList<>();
    private static DescriptionMaterial[] values;
    private static final Map<String, DescriptionMaterial> BY_NAME = new HashMap<>();

    private static final Supplier<TextureAtlas> PARTICLES_ATLAS = () -> atlas(TextureAtlas.LOCATION_PARTICLES);
    private static final Supplier<TextureAtlas> BLOCKS_ATLAS = () -> atlas(TextureAtlas.LOCATION_BLOCKS);

    public static final DescriptionMaterial
            TERRAIN_SHEET = register("terrain_sheet", BLOCKS_ATLAS),
            PARTICLE_SHEET_OPAQUE = register("particle_sheet_opaque", PARTICLES_ATLAS),
            PARTICLE_SHEET_TRANSLUCENT = register("particle_sheet_translucent", PARTICLES_ATLAS),
            PARTICLE_SHEET_LIT = register("particle_sheet_lit", PARTICLES_ATLAS),
            CUSTOM = register("custom", PARTICLES_ATLAS),
            NO_RENDER = register("no_renderer", PARTICLES_ATLAS),
            particles_alpha = register("particles_alpha", PARTICLES_ATLAS),
            particles_blend = register("particles_blend", PARTICLES_ATLAS),
            particles_add = register("particles_add", PARTICLES_ATLAS),
            particles_opaque = register("particles_opaque", PARTICLES_ATLAS);

    @ApiStatus.Internal
    public static DescriptionMaterial register(String name, @Nullable Supplier<TextureAtlas> atlas) {
        DescriptionMaterial material = new DescriptionMaterial(name, atlas);
        list.add(material);
        return material;
    }

    /// 所有已注册的材质（首次调用时完成自定义材质注册）。
    @ApiStatus.Internal
    public static synchronized DescriptionMaterial[] values() {
        if (values == null) {
            // 先放进内置材质再触发事件：事件处理器里如果又碰到材质反序列化（values() 重入），
            // 会直接拿到这份临时数组，而不是看到 list 已被清空。
            values = list.toArray(new DescriptionMaterial[0]);
            for (DescriptionMaterial material : values) {
                BY_NAME.put(material.getSerializedName(), material);
            }
            ModLoader.get().postEvent(new RegisterCustomMaterialEvent(DescriptionMaterial::register));
            values = list.toArray(new DescriptionMaterial[0]);
            for (DescriptionMaterial material : values) {
                BY_NAME.put(material.getSerializedName(), material);
            }
        }
        return values;
    }

    private final String name;
    private final @Nullable Supplier<TextureAtlas> atlas;

    private DescriptionMaterial(String name, @Nullable Supplier<TextureAtlas> atlas) {
        this.name = name;
        this.atlas = atlas;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /// 该材质对应的纹理图集（懒取）。自定义材质没给 supplier、或图集还没加载出来时回落到粒子图集。
    public TextureAtlas getAtlas() {
        TextureAtlas textureAtlas = this.atlas == null ? null : this.atlas.get();
        if (textureAtlas != null) {
            return textureAtlas;
        }
        TextureAtlas particles = atlas(TextureAtlas.LOCATION_PARTICLES);
        if (particles == null) {
            throw new IllegalStateException("Particles texture atlas is not loaded yet");
        }
        return particles;
    }

    private static @Nullable TextureAtlas atlas(ResourceLocation location) {
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(location);
        return texture instanceof TextureAtlas textureAtlas ? textureAtlas : null;
    }

    public static final Codec<DescriptionMaterial> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<DescriptionMaterial, T>> decode(DynamicOps<T> ops, T input) {
            return Codec.STRING.decode(ops, input).map(p -> {
                values();
                return new Pair<>(BY_NAME.getOrDefault(p.getFirst(), CUSTOM), p.getSecond());
            });
        }

        @Override
        public <T> DataResult<T> encode(DescriptionMaterial input, DynamicOps<T> ops, T prefix) {
            return Codec.STRING.encode(input.getSerializedName(), ops, prefix);
        }
    };
}

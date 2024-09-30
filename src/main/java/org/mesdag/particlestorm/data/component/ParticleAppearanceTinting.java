package org.mesdag.particlestorm.data.component;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public record ParticleAppearanceTinting(Color color, ColorField colorField) implements IParticleComponent {
    public static final Codec<ParticleAppearanceTinting> CODEC = Codec.either(Color.CODEC, ColorField.CODEC).xmap(
            either -> either.map(c -> new ParticleAppearanceTinting(c, ColorField.EMPTY), f -> new ParticleAppearanceTinting(Color.EMPTY, f)),
            tinting -> tinting.colorField == ColorField.EMPTY ? Either.left(tinting.color) : Either.right(tinting.colorField)
    ).fieldOf("color").codec();

    @Override
    public Codec<ParticleAppearanceTinting> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        List<FloatMolangExp> collect = new ArrayList<>(color.gradient.map.values().stream().flatMap(field -> Stream.of(field.red, field.green, field.blue, field.alpha)).toList());
        collect.add(color.interpolant);
        collect.add(colorField.red);
        collect.add(colorField.green);
        collect.add(colorField.blue);
        collect.add(colorField.alpha);
        return List.copyOf(collect);
    }

    @Override
    public void update(MolangParticleInstance instance) {
        // todo
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    public record Color(Gradient gradient, FloatMolangExp interpolant) {
        public static final Color EMPTY = new Color(Gradient.EMPTY, FloatMolangExp.ZERO);
        public static final Codec<Color> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Gradient.CODEC.fieldOf("gradient").orElse(Gradient.EMPTY).forGetter(Color::gradient),
                FloatMolangExp.CODEC.fieldOf("interpolant").orElse(FloatMolangExp.ZERO).forGetter(Color::interpolant)
        ).apply(instance, Color::new));

        public record Gradient(Map<String, ColorField> map) {
            public static final Gradient EMPTY = new Gradient(Map.of());
            public static final Codec<Gradient> CODEC = Codec.either(Codec.list(ColorField.CODEC), Codec.unboundedMap(Codec.STRING, ColorField.CODEC)).xmap(
                    either -> either.map(l -> {
                        float step = 1.0F / l.size();
                        Iterator<ColorField> iterator = l.iterator();
                        ImmutableMap.Builder<String, ColorField> builder = ImmutableMap.builder();
                        int i = 0;
                        while (iterator.hasNext()) {
                            ColorField field = iterator.next();
                            if (iterator.hasNext()) {
                                builder.put(Float.toString(i * step), field);
                            } else {
                                builder.put("1.0", field);
                            }
                            i++;
                        }
                        return new Gradient(builder.build());
                    }, Gradient::new),
                    gradient -> Either.right(gradient.map)
            );
        }
    }

    public record ColorField(FloatMolangExp red, FloatMolangExp green, FloatMolangExp blue, FloatMolangExp alpha) {
        public static final ColorField EMPTY = new ColorField(FloatMolangExp.ZERO, FloatMolangExp.ZERO, FloatMolangExp.ZERO, FloatMolangExp.ZERO);
        public static final Codec<ColorField> CODEC = Codec.either(Codec.STRING, Codec.list(FloatMolangExp.CODEC, 3, 4)).xmap(
                either -> either.map(hex -> {
                    hex = hex.replace("#", "");
                    if (hex.length() != 6 && hex.length() != 8) throw new IllegalArgumentException("The size is not allowed");
                    float r = Integer.parseInt(hex.substring(0, 2), 16) / 255.0F;
                    float g = Integer.parseInt(hex.substring(2, 4), 16) / 255.0F;
                    float b = Integer.parseInt(hex.substring(4, 6), 16) / 255.0F;
                    float a = 1.0F;
                    if (hex.length() == 8) {
                        a = Integer.parseInt(hex.substring(6, 8), 16) / 255.0F;
                    }
                    return new ColorField(FloatMolangExp.ofConstant(r), FloatMolangExp.ofConstant(g), FloatMolangExp.ofConstant(b), FloatMolangExp.ofConstant(a));
                }, exps -> new ColorField(exps.getFirst(), exps.get(1), exps.get(2), exps.size() == 4 ? exps.get(3) : FloatMolangExp.ONE)),
                field -> Either.right(List.of(field.red, field.green, field.blue, field.alpha))
        );
    }
}

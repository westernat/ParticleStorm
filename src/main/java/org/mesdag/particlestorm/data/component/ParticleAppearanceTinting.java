package org.mesdag.particlestorm.data.component;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.MolangInstance;
import org.mesdag.particlestorm.particle.MolangParticleInstance;

import java.util.*;
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
        apply(instance);
    }

    private float[] getCalculatedColor(MolangParticleInstance instance, ArrayList<Tuple<Float, ColorField>> list, float ratio) {
        int n = 0;
        for (int index = 0; index < list.size(); index++) {
            Tuple<Float, ColorField> tuple = list.get(index);
            if (tuple.getA() <= ratio) {
                n = index;
            }
        }
        Tuple<Float, ColorField> tuple = list.get(n);
        if ((n == 0 && list.size() == 1) || n == list.size() - 1) {
            return tuple.getB().calculate(instance);
        }
        Tuple<Float, ColorField> next = list.get(n + 1);
        float[] color = tuple.getB().calculate(instance);
        float[] another = next.getB().calculate(instance);
        float percent = (ratio - tuple.getA()) / (next.getA() - tuple.getA());
        float r = Mth.clamp(color[0] - (color[0] - another[0]) * percent, 0.0F, 1.0F);
        float g = Mth.clamp(color[1] - (color[1] - another[1]) * percent, 0.0F, 1.0F);
        float b = Mth.clamp(color[2] - (color[2] - another[2]) * percent, 0.0F, 1.0F);
        float a = Mth.clamp(color[3] - (color[3] - another[3]) * percent, 0.0F, 1.0F);
        return new float[]{r, g, b, a};
    }

    @Override
    public void apply(MolangParticleInstance instance) {
        if (color.interpolant.initialized() && !color.gradient.map.isEmpty()) {
            float interpolant = color.interpolant.calculate(instance);
            float[] calculated = getCalculatedColor(instance, color.gradient.list, interpolant / color.gradient.range);
            instance.setColor(calculated[0], calculated[1], calculated[2], calculated[3]);
        } else {
            float[] color = colorField.calculate(instance);
            instance.setColor(color[0], color[1], color[2], color[3]);
        }
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public String toString() {
        return "ParticleAppearanceTinting{" +
                "color=" + color +
                ", colorField=" + colorField +
                '}';
    }

    public record Color(Gradient gradient, FloatMolangExp interpolant) {
        public static final Color EMPTY = new Color(Gradient.EMPTY, FloatMolangExp.ZERO);
        public static final Codec<Color> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Gradient.CODEC.fieldOf("gradient").orElse(Gradient.EMPTY).forGetter(Color::gradient),
                FloatMolangExp.CODEC.fieldOf("interpolant").orElse(FloatMolangExp.ZERO).forGetter(Color::interpolant)
        ).apply(instance, Color::new));

        @Override
        public String toString() {
            return "Color{" +
                    "gradient=" + gradient +
                    ", interpolant=" + interpolant +
                    '}';
        }

        public static final class Gradient {
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
            public final Map<String, ColorField> map;

            public final float range;
            public final ArrayList<Tuple<Float, ColorField>> list;

            public Gradient(Map<String, ColorField> map) {
                this.map = map;
                this.list = new ArrayList<>();
                map.entrySet().stream()
                        .map(entry -> new Tuple<>(Float.parseFloat(entry.getKey()), entry.getValue()))
                        .sorted(Comparator.comparing(Tuple::getA))
                        .forEachOrdered(list::add);
                this.range = list.isEmpty() ? 0.0F : list.getLast().getA();
            }

            @Override
            public String toString() {
                return "Gradient{" +
                        "map=" + map +
                        '}';
            }
        }
    }

    public record ColorField(FloatMolangExp red, FloatMolangExp green, FloatMolangExp blue, FloatMolangExp alpha) {
        public static final ColorField EMPTY = new ColorField(FloatMolangExp.ZERO, FloatMolangExp.ZERO, FloatMolangExp.ZERO, FloatMolangExp.ZERO);
        public static final Codec<ColorField> CODEC = Codec.either(Codec.STRING, Codec.list(FloatMolangExp.CODEC, 3, 4)).xmap(
                either -> either.map(hex -> {
                    hex = hex.replace("#", "");
                    if (hex.length() != 6 && hex.length() != 8) throw new IllegalArgumentException("The size is not allowed");
                    float a, r, g, b;
                    float v0 = Integer.parseInt(hex.substring(0, 2), 16) / 255.0F;
                    float v1 = Integer.parseInt(hex.substring(2, 4), 16) / 255.0F;
                    float v2 = Integer.parseInt(hex.substring(4, 6), 16) / 255.0F;
                    if (hex.length() == 6) {
                        a = 1.0F;
                        r = v0;
                        g = v1;
                        b = v2;
                    } else {
                        a = v0;
                        r = v1;
                        g = v2;
                        b = Integer.parseInt(hex.substring(6, 8), 16) / 255.0F;
                    }
                    return new ColorField(FloatMolangExp.ofConstant(r), FloatMolangExp.ofConstant(g), FloatMolangExp.ofConstant(b), FloatMolangExp.ofConstant(a));
                }, exps -> new ColorField(exps.getFirst(), exps.get(1), exps.get(2), exps.size() == 4 ? exps.get(3) : FloatMolangExp.ONE)),
                field -> Either.right(List.of(field.red, field.green, field.blue, field.alpha))
        );

        public float[] calculate(MolangInstance instance) {
            return new float[]{
                    red.calculate(instance),
                    green.calculate(instance),
                    blue.calculate(instance),
                    alpha.calculate(instance)
            };
        }

        @Override
        public String toString() {
            return "ColorField{" +
                    "red=" + red +
                    ", green=" + green +
                    ", blue=" + blue +
                    ", alpha=" + alpha +
                    '}';
        }
    }
}

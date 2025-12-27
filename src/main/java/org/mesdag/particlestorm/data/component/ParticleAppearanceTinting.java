package org.mesdag.particlestorm.data.component;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import org.mesdag.particlestorm.api.Deserializer;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.*;
import java.util.stream.Stream;

public record ParticleAppearanceTinting(Color color, ColorField colorField) implements IParticleComponent {
    @Override
    public Deserializer<ParticleAppearanceTinting> deserializer() {
        return ParticleAppearanceTinting::fromJson;
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
    public void update(IMolangParticleInstance instance) {
        apply(instance);
    }

    private float[] getCalculatedColor(IMolangParticleInstance instance, ArrayList<Tuple<Float, ColorField>> list, float ratio) {
        int n = 0;
        for (int index = 0; index < list.size(); index++) {
            Tuple<Float, ColorField> tuple = list.get(index);
            if (tuple.getA() <= ratio) {
                n = index;
            } else {
                break;
            }
        }
        Tuple<Float, ColorField> tuple = list.get(n);
        if ((n == 0 && list.size() == 1) || n == list.size() - 1) {
            return tuple.getB().calculate(instance);
        }
        Tuple<Float, ColorField> next = list.get(n + 1);
        float[] color = tuple.getB().calculate(instance);
        float[] another = next.getB().calculate(instance);
        float factor = 1.0F - (ratio - tuple.getA()) / (next.getA() - tuple.getA());
        float r = mix(color[0], another[0], factor);
        float g = mix(color[1], another[1], factor);
        float b = mix(color[2], another[2], factor);
        float a = mix(color[3], another[3], factor);
        return new float[]{r, g, b, a};
    }

    private float mix(float first, float second, float ratio) {
        return Mth.clamp((first - second) * ratio + second, 0.0F, 1.0F);
    }

    @Override
    public void apply(IMolangParticleInstance instance) {
        if (color.interpolant.initialized() && !color.gradient.map.isEmpty()) {
            float interpolant = color.interpolant.calculate(instance);
            float[] calculated = getCalculatedColor(instance, color.gradient.list, interpolant / color.gradient.range);
            instance.setColor(calculated[0], calculated[1], calculated[2], calculated[3]);
        } else {
            float[] color = colorField.calculate(instance);
            instance.setColor(
                    Mth.clamp(color[0], 0.0F, 1.0F),
                    Mth.clamp(color[1], 0.0F, 1.0F),
                    Mth.clamp(color[2], 0.0F, 1.0F),
                    Mth.clamp(color[3], 0.0F, 1.0F)
            );
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

    public static ParticleAppearanceTinting fromJson(JsonElement element) {
        JsonElement color1 = element.getAsJsonObject().get("color");
        try {
            return new ParticleAppearanceTinting(Color.EMPTY, ColorField.fromJson(color1));
        } catch (Exception e) {
            return new ParticleAppearanceTinting(Color.fromJson(color1), ColorField.EMPTY);
        }
    }

    public record Color(Gradient gradient, FloatMolangExp interpolant) {
        public static final Color EMPTY = new Color(Gradient.EMPTY, FloatMolangExp.ZERO);

        @Override
        public String toString() {
            return "Color{" +
                    "gradient=" + gradient +
                    ", interpolant=" + interpolant +
                    '}';
        }

        public static Color fromJson(JsonElement element) {
            JsonObject object = element.getAsJsonObject();
            JsonElement gradient1 = object.get("gradient");
            Gradient gradient2 = gradient1 == null ? Gradient.EMPTY : Gradient.fromJson(gradient1);
            FloatMolangExp exp = FloatMolangExp.fromJson(object.get("interpolant"));
            return new Color(gradient2, exp);
        }

        public static final class Gradient {
            public static final Gradient EMPTY = new Gradient(Map.of());
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
                this.range = list.isEmpty() ? 0.0F : list.get(list.size() - 1).getA();
            }

            @Override
            public String toString() {
                return "Gradient{" +
                        "map=" + map +
                        '}';
            }

            public static Gradient fromJson(JsonElement element) {
                if (element.isJsonArray()) {
                    JsonArray array = element.getAsJsonArray();
                    float step = 1.0F / array.size();
                    Iterator<JsonElement> iterator = array.iterator();
                    ImmutableMap.Builder<String, ColorField> builder = ImmutableMap.builder();
                    int i = 0;
                    while (iterator.hasNext()) {
                        JsonElement element1 = iterator.next();
                        if (iterator.hasNext()) {
                            builder.put(Float.toString(i * step), ColorField.fromJson(element1));
                        } else {
                            builder.put("1.0", ColorField.fromJson(element1));
                        }
                        i++;
                    }
                    return new Gradient(builder.build());
                }
                if (element.isJsonObject()) {
                    ImmutableMap.Builder<String, ColorField> builder = ImmutableMap.builder();
                    for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                        builder.put(entry.getKey(), ColorField.fromJson(entry.getValue()));
                    }
                    return new Gradient(builder.build());
                }
                throw new JsonParseException("Not a(n) array or object: " + element);
            }
        }
    }

    public record ColorField(FloatMolangExp red, FloatMolangExp green, FloatMolangExp blue, FloatMolangExp alpha) {
        public static final ColorField EMPTY = new ColorField(FloatMolangExp.ZERO, FloatMolangExp.ZERO, FloatMolangExp.ZERO, FloatMolangExp.ZERO);

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

        public static ColorField fromJson(JsonElement element) {
            if (element.isJsonPrimitive()) {
                String hex = element.getAsString();
                hex = hex.replace("#", "");
                if (hex.length() != 6 && hex.length() != 8) {
                    throw new JsonParseException("Not a valid color string: " + hex);
                }
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
            }
            if (element.isJsonArray()) {
                JsonArray array = element.getAsJsonArray();
                if (array.size() != 3 && array.size() != 4) {
                    throw new JsonParseException("array color must in size of 3 or 4: " + array);
                }
                FloatMolangExp exp0 = FloatMolangExp.fromJson(array.get(0));
                FloatMolangExp exp1 = FloatMolangExp.fromJson(array.get(1));
                FloatMolangExp exp2 = FloatMolangExp.fromJson(array.get(2));
                FloatMolangExp exp3 = array.size() == 4 ? FloatMolangExp.fromJson(array.get(3)) : FloatMolangExp.ONE;
                return new ColorField(exp0, exp1, exp2, exp3);
            }
            throw new JsonParseException("Not a(n) string or array: " + element);
        }
    }
}

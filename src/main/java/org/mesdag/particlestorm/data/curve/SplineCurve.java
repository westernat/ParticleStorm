package org.mesdag.particlestorm.data.curve;

import net.minecraft.util.Mth;

public abstract class SplineCurve {
    public abstract float getPoint(float e);

    public static class CatMullRom extends SplineCurve {
        private final float[] points;

        public CatMullRom(float... points) {
            this.points = points;
        }

        @Override
        public float getPoint(float e) {
            int size = points.length;
            int i1 = size - 1;
            float i = i1 * e;
            int a = Mth.floor(i);
            float o = i - a;
            float s = points[a == 0 ? a : a - 1];
            float l = points[a];
            float c = points[a > size - 2 ? i1 : a + 1];
            float u = points[a > size - 3 ? i1 : a + 2];
            return getDirectPoint(o, s, l, c, u);
        }

        public static float getDirectPoint(float e, float t, float n, float r, float i) {
            float a = 0.5F * (r - t);
            float o = 0.5F * (i - n);
            float s = e * e;
            return (2 * n - 2 * r + a + o) * (e * s) + (-3 * n + 3 * r - 2 * a - o) * s + a * e + n;
        }
    }

    public static class Bezier extends SplineCurve {
        private final float v0;
        private final float v1;
        private final float v2;
        private final float v3;

        public Bezier(float[] points) {
            int length = points.length;
            this.v0 = length > 0 ? points[0] : 0.0F;
            this.v1 = length > 1 ? points[1] : 0.0F;
            this.v2 = length > 2 ? points[2] : 0.0F;
            this.v3 = length > 3 ? points[3] : 0.0F;
        }

        @Override
        public float getPoint(float e) {
            return getDirectPoint(e, v0, v1, v2, v3);
        }

        public static float getDirectPoint(float e, float t, float n, float r, float i) {
            float n1 = 1 - e;
            float v = n1 * n1;
            float a = v * n1 * t;
            float b = 3 * v * e * n;
            float v4 = e * e;
            float c = 3 * n1 * v4 * r;
            float d = v4 * e * i;
            return a + b + c + d;
        }
    }
}

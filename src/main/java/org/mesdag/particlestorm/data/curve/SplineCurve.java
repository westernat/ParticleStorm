package org.mesdag.particlestorm.data.curve;

import net.minecraft.util.Mth;
import org.joml.Vector2f;

import java.util.ArrayList;

public abstract class SplineCurve {
    public abstract Vector2f getPoint(float e, Vector2f t);

    public Vector2f getPoint(float e) {
        return getPoint(e, new Vector2f());
    }

    public static class CatMullRom extends SplineCurve {
        private final ArrayList<Vector2f> points;

        public CatMullRom(ArrayList<Vector2f> points) {
            this.points = points;
        }

        public Vector2f getPoint(float e, Vector2f t) {
            float i = (points.size() - 1) * e;
            int a = Mth.floor(i);
            float o = i - a;
            Vector2f s = points.get(0 == a ? a : a - 1);
            Vector2f l = points.get(a);
            Vector2f c = points.get(a > points.size() - 2 ? points.size() - 1 : a + 1);
            Vector2f u = points.get(a > points.size() - 3 ? points.size() - 1 : a + 2);
            return t.set(p(o, s.x, l.x, c.x, u.x), p(o, s.y, l.y, c.y, u.y));
        }

        public float p(float e, float t, float n, float r, float i) {
            float a = 0.5F * (r - t);
            float o = 0.5F * (i - n);
            float s = e * e;
            return (2 * n - 2 * r + a + o) * (e * s) + (-3 * n + 3 * r - 2 * a - o) * s + a * e + n;
        }
    }

    public static class Bezier extends SplineCurve {
        private final Vector2f v0;
        private final Vector2f v1;
        private final Vector2f v2;
        private final Vector2f v3;

        public Bezier(Vector2f v0, Vector2f v1, Vector2f v2, Vector2f v3) {
            this.v0 = v0;
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
        }

        @Override
        public Vector2f getPoint(float e, Vector2f t) {
            return t.set(i(e, v0.x, v1.x, v2.x, v3.x), i(e, v0.y, v1.y, v2.y, v3.y));
        }

        public float i(float e, float t, float n, float r, float i) {
            float n1 = 1 - e;
            float a = n1 * n1 * n1 * t;
            float b = 3 * n1 * n1 * e * n;
            float c = 3 * n1 * e * e * r;
            float d = e * e * e * i;
            return a + b + c + d;
        }
    }
}

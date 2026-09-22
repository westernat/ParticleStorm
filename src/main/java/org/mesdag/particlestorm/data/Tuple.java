package org.mesdag.particlestorm.data;

/**
 * Minimal replacement for net.minecraft.util.Tuple, which was removed in Minecraft 26.2.
 * Keeps the getA()/getB() accessor names so existing call sites stay unchanged.
 */
public record Tuple<A, B>(A a, B b) {
    public A getA() {
        return a;
    }

    public B getB() {
        return b;
    }
}

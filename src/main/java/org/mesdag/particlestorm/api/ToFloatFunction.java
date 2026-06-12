package org.mesdag.particlestorm.api;

@FunctionalInterface
public interface ToFloatFunction<T> {
    float applyAsFloat(T t);
}

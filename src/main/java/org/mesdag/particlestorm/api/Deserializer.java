package org.mesdag.particlestorm.api;

import com.google.gson.JsonElement;

public interface Deserializer<A> {
    A fromJson(JsonElement element);
}

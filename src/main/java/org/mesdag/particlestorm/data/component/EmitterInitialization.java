package org.mesdag.particlestorm.data.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.mesdag.particlestorm.api.Deserializer;
import org.mesdag.particlestorm.api.IEmitterComponent;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.util.List;

/// This component allows the emitter to run some Molang at creation, primarily to populate any Molang variables that get used later.
///
/// @param creationExpression This is run once at emitter startup
/// @param perUpdateExpression This is run once per emitter update
public record EmitterInitialization(MolangExp creationExpression, MolangExp perUpdateExpression) implements IEmitterComponent {
    @Override
    public Deserializer<EmitterInitialization> deserializer() {
        return EmitterInitialization::fromJson;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(creationExpression, perUpdateExpression);
    }

    @Override
    public void update(ParticleEmitter entity) {
        perUpdateExpression.calculate(entity);
    }

    @Override
    public void apply(ParticleEmitter entity) {
        creationExpression.calculate(entity);
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public int order() {
        return 500;
    }

    @Override
    public String toString() {
        return "EmitterInitialization{" +
                "creationExpression=" + creationExpression +
                ", perUpdateExpression=" + perUpdateExpression +
                '}';
    }

    public static EmitterInitialization fromJson(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        return new EmitterInitialization(
                MolangExp.fromJson(object.get("creation_expression")),
                MolangExp.fromJson(object.get("per_update_expression"))
        );
    }
}

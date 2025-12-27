package org.mesdag.particlestorm.data.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.util.GsonHelper;
import org.mesdag.particlestorm.api.Deserializer;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.List;

/// Starts the particle with a specified render expression.
public record ParticleInitialization(FloatMolangExp perRenderExpression) implements IParticleComponent {
    @Override
    public Deserializer<ParticleInitialization> deserializer() {
        return ParticleInitialization::fromJson;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(perRenderExpression);
    }

    @Override
    public void update(IMolangParticleInstance instance) {
        perRenderExpression.calculate(instance);
    }

    @Override
    public void apply(IMolangParticleInstance instance) {
        perRenderExpression.calculate(instance);
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public int order() {
        return 400;
    }

    @Override
    public String toString() {
        return "ParticleInitialization{" +
                "perRenderExpression=" + perRenderExpression +
                '}';
    }

    public static ParticleInitialization fromJson(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        if (object.has("per_update_expression")) {
            throw new JsonParseException("per_update_expression is not allowed here, please use per_render_expression instead");
        }
        return new ParticleInitialization(FloatMolangExp.fromJson(GsonHelper.getNonNull(object, "per_render_expression")));
    }
}

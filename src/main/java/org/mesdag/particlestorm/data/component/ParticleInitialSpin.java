package org.mesdag.particlestorm.data.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.Mth;
import org.mesdag.particlestorm.api.Deserializer;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.molang.FloatMolangExp;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.List;

/// Starts the particle with a specified orientation and rotation rate.
///
/// @param rotation     Specifies the initial rotation in degrees. Evaluated once
/// @param rotationRate Specifies the spin rate in degrees/second. Evaluated once
public record ParticleInitialSpin(FloatMolangExp rotation, FloatMolangExp rotationRate) implements IParticleComponent {
    @Override
    public Deserializer<ParticleInitialSpin> deserializer() {
        return ParticleInitialSpin::fromJson;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of(rotation, rotationRate);
    }

    @Override
    public void apply(IMolangParticleInstance instance) {
        instance.setZRot(rotation.calculate(instance) * Mth.DEG_TO_RAD);
        instance.setZRotD(rotationRate.calculate(instance) * instance.getInvTickRate() * Mth.DEG_TO_RAD);
    }

    @Override
    public int order() {
        return 500;
    }

    public static ParticleInitialSpin fromJson(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        FloatMolangExp exp = FloatMolangExp.fromJson(object.get("rotation"));
        FloatMolangExp exp1 = FloatMolangExp.fromJson(object.get("rotation_rate"));
        return new ParticleInitialSpin(exp, exp1);
    }
}

package org.mesdag.particlestorm.data.component;

import com.google.gson.JsonElement;
import org.mesdag.particlestorm.api.Deserializer;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.List;

/// When this component exists, particle will be tinted by local lighting conditions in-game.
public final class ParticleAppearanceLighting implements IParticleComponent {
    public static final ParticleAppearanceLighting INSTANCE = new ParticleAppearanceLighting();

    private ParticleAppearanceLighting() {}

    @Override
    public Deserializer<ParticleAppearanceLighting> deserializer() {
        return ParticleAppearanceLighting::fromJson;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of();
    }

    public static ParticleAppearanceLighting fromJson(JsonElement element) {
        return INSTANCE;
    }
}

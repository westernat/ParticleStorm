package org.mesdag.particlestorm.integration.geckolib;

import org.mesdag.particlestorm.ParticleStorm;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class ReplacedCreeperModel extends DefaultedEntityGeoModel<ReplacedCreeperEntity> {
    public ReplacedCreeperModel() {
        super(ParticleStorm.asResource("creeper"));
    }
}

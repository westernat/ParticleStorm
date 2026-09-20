package org.mesdag.particlestorm.compat.iris;

import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;
import org.mesdag.particlestorm.PSGameClient;

public final class IrisParticlePipelines {
    private IrisParticlePipelines() {
    }

    public static void register() {
        // Custom pipelines need Iris routing to the shader pack's particle render targets.
        IrisApi iris = IrisApi.getInstance();
        iris.assignPipeline(PSGameClient.PARTICLE_ADD.pipeline(), IrisProgram.PARTICLES_TRANSLUCENT);
        iris.assignPipeline(PSGameClient.PARTICLE_BLEND.pipeline(), IrisProgram.PARTICLES_TRANSLUCENT);
    }
}

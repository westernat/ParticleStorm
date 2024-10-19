package org.mesdag.particlestorm.integration.geckolib;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.mesdag.particlestorm.ParticleStorm;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class ExampleBlockEntityRenderer extends GeoBlockRenderer<TestBlock.ExampleBlockEntity> {
    public ExampleBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(new DefaultedBlockGeoModel<>(ParticleStorm.asResource("test_block")));
    }
}

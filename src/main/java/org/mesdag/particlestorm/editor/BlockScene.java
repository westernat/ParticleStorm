package org.mesdag.particlestorm.editor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import java.util.List;

import static net.minecraft.client.renderer.LevelRenderer.DIRECTIONS;

class BlockScene {
    static final int[] LIGHTMAP = {LightTexture.FULL_BRIGHT, LightTexture.FULL_BRIGHT, LightTexture.FULL_BRIGHT, LightTexture.FULL_BRIGHT};
    static final RandomSource random1 = RandomSource.create();
    static final RandomSource random2 = RandomSource.create();
    static final BlockPos.MutableBlockPos offset = new BlockPos.MutableBlockPos();

    static void tesselateWithoutAO(Minecraft minecraft, BlockState state, BlockPos pos, PoseStack.Pose pose, boolean checkSides) {
        long seed = state.getSeed(pos);
        BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);
        ModelData modelData = ModelData.EMPTY;

        random2.setSeed(seed);
        for (RenderType renderType : model.getRenderTypes(state, random2, modelData)) {
            VertexConsumer consumer = minecraft.renderBuffers().bufferSource().getBuffer(renderType);
            for (Direction direction : DIRECTIONS) {
                random1.setSeed(seed);
                List<BakedQuad> quads = model.getQuads(state, direction, random1, modelData, renderType);
                if (quads.isEmpty()) continue;
                offset.setWithOffset(pos, direction);
                if (!checkSides || Block.shouldRenderFace(state, minecraft.level, pos, direction, offset)) {
                    renderModelFaceFlat(minecraft, state, pos, pose, consumer, quads);
                }
            }

            random1.setSeed(seed);
            List<BakedQuad> quads = model.getQuads(state, null, random1, modelData, renderType);
            if (quads.isEmpty()) continue;
            renderModelFaceFlat(minecraft, state, pos, pose, consumer, quads);
        }
    }

    private static void renderModelFaceFlat(Minecraft minecraft, BlockState state, BlockPos pos, PoseStack.Pose pose, VertexConsumer consumer, List<BakedQuad> quads) {
        for (BakedQuad bakedquad : quads) {
            float shade = minecraft.level.getShade(bakedquad.getDirection(), bakedquad.isShade());
            float r;
            float g;
            float b;
            if (bakedquad.isTinted()) {
                int i = minecraft.getBlockColors().getColor(state, minecraft.level, pos, bakedquad.getTintIndex());
                r = (float) (i >> 16 & 255) / 255.0F;
                g = (float) (i >> 8 & 255) / 255.0F;
                b = (float) (i & 255) / 255.0F;
            } else {
                r = 1.0F;
                g = 1.0F;
                b = 1.0F;
            }
            consumer.putBulkData(pose, bakedquad, new float[]{shade, shade, shade, shade}, r, g, b, 1.0F, LIGHTMAP, OverlayTexture.NO_OVERLAY, true);
        }
    }
}

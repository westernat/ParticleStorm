package org.mesdag.particlestorm.integration.geckolib;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.ParticleStorm;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TestBlock extends Block implements EntityBlock {

    public static final VoxelShape BOX = Shapes.box(0.1, 0.1, 0.1, 0.9, 0.9, 0.9);

    public TestBlock() {
        super(Properties.of());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ExampleBlockEntity(pos, state);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return BOX;
    }

    public static class ExampleBlockEntity extends BlockEntity implements GeoBlockEntity {
        protected static final RawAnimation DEPLOY_ANIM = RawAnimation.begin().thenLoop("animation.test_block.new");

        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        public ExampleBlockEntity(BlockPos pos, BlockState state) {
            super(ParticleStorm.TEST_ENTITY.get(), pos, state);
        }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<>(this, state -> state.setAndContinue(DEPLOY_ANIM)));
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return this.cache;
        }
    }
}

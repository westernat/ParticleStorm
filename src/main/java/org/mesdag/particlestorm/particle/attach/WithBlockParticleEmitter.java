package org.mesdag.particlestorm.particle.attach;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.data.event.ParticleEffect;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.ParticleEmitter;

public class WithBlockParticleEmitter extends IgnoreRangeParticleEmitter {
    public static final ResourceLocation TYPE = ParticleStorm.asResource("with_block");

    protected @Nullable WithBlockParticleEmitter.BlockData blockData;

    public WithBlockParticleEmitter(Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression, boolean ignoreSameBlock, boolean ignoreRange) {
        super(TYPE, level, pos, particleId, expression, ignoreRange);
        initBlock(level, pos, ignoreSameBlock);
    }

    public WithBlockParticleEmitter(Level level, CompoundTag tag) {
        super(level, tag);
        initBlock(level, pos, tag.getBoolean("ignoreSameBlock"));
    }

    public WithBlockParticleEmitter(ParticleEmitter parent, ParticleEffect effect) {
        super(parent, effect);
        initBlock(parent.level, parent.getPosition(), parent instanceof WithBlockParticleEmitter wbpe && wbpe.blockData != null && wbpe.blockData.ignoreSameBlock);
    }

    private void initBlock(Level level, Vec3 pos, boolean ignoreSameBlock) {
        BlockPos blockPos = BlockPos.containing(pos);
        BlockState state = level.getBlockState(blockPos);
        if (state.isAir()) {
            remove();
        } else {
            this.blockData = new BlockData(blockPos, state, ignoreSameBlock);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (blockData != null) {
            BlockState state = level.getBlockState(blockData.pos);
            if (state != blockData.state && (!blockData.ignoreSameBlock || state.getBlock() != blockData.state.getBlock())) {
                remove();
            }
        }
    }

    @Override
    public boolean isRemoved() {
        return super.isRemoved() || blockData == null;
    }

    @Override
    public void serialize(CompoundTag tag) {
        super.serialize(tag);
        if (blockData != null) {
            tag.putBoolean("ignoreSameBlock", blockData.ignoreSameBlock);
        }
    }

    @Override
    public void deserialize(CompoundTag tag) {
        super.deserialize(tag);

    }

    public record BlockData(BlockPos pos, BlockState state, boolean ignoreSameBlock) {}
}

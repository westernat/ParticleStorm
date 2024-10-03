package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.state.BlockState;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// todo

public record ParticleExpireIfNotInBlocks(Set<BlockState> blocks) implements IParticleComponent {
    public static final Codec<ParticleExpireIfNotInBlocks> CODEC = Codec.list(BlockState.CODEC).xmap(
            states -> new ParticleExpireIfNotInBlocks(new HashSet<>(states)),
            blocks -> List.copyOf(blocks.blocks)
    );

    @Override
    public Codec<ParticleExpireIfNotInBlocks> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of();
    }
}

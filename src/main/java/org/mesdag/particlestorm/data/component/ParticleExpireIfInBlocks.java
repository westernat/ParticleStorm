package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.state.BlockState;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// todo

/**
 * Particles expire when in a block of the type in the list.
 * <p>
 * Note: this component can exist alongside particle_lifetime_expression.
 */
public record ParticleExpireIfInBlocks(Set<BlockState> blocks) implements IParticleComponent {
    public static final Codec<ParticleExpireIfInBlocks> CODEC = Codec.list(BlockState.CODEC).xmap(
            states -> new ParticleExpireIfInBlocks(new HashSet<>(states)),
            blocks -> List.copyOf(blocks.blocks)
    );

    @Override
    public Codec<ParticleExpireIfInBlocks> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of();
    }
}

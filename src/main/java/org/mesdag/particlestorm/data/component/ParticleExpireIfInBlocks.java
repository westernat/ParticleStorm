package org.mesdag.particlestorm.data.component;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Particles expire when in a block of the type in the list.<p>
 * Note: this component can exist alongside particle_lifetime_expression.
 */
public final class ParticleExpireIfInBlocks implements IParticleComponent {
    public static final Codec<ParticleExpireIfInBlocks> CODEC = Codec.list(Codec.STRING).xmap(
            states -> new ParticleExpireIfInBlocks(new HashSet<>(states)),
            blocks -> List.copyOf(blocks.ids)
    );
    public final Set<String> ids;
    public final Set<BlockState> states;

    public ParticleExpireIfInBlocks(Set<String> ids) {
        this.ids = ids;
        this.states = new HashSet<>();
    }

    @Override
    public Codec<ParticleExpireIfInBlocks> codec() {
        return CODEC;
    }

    @Override
    public List<MolangExp> getAllMolangExp() {
        return List.of();
    }

    @Override
    public void initialize(Level level) {
        if (states.isEmpty()) {
            try {
                HolderLookup<Block> lookup = level.holderLookup(Registries.BLOCK);
                for (String id : ids) {
                    BlockStateParser.BlockResult result = BlockStateParser.parseForBlock(lookup, id, false);
                    states.add(result.blockState());
                }
            } catch (CommandSyntaxException e) {
                states.add(Blocks.AIR.defaultBlockState());
                ParticleStorm.LOGGER.error(e.getMessage());
            }
        }
    }

    @Override
    public String toString() {
        return "ParticleExpireIfInBlocks[" +
                "blocks=" + ids + ']';
    }
}

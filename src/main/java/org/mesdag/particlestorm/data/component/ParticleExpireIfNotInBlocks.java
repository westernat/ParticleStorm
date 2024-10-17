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
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ParticleExpireIfNotInBlocks implements IParticleComponent {
    public static final Codec<ParticleExpireIfNotInBlocks> CODEC = Codec.list(Codec.STRING).xmap(
            states -> new ParticleExpireIfNotInBlocks(new HashSet<>(states)),
            blocks -> List.copyOf(blocks.ids)
    );
    private final Set<String> ids;
    public final Set<BlockState> states;

    public ParticleExpireIfNotInBlocks(Set<String> ids) {
        this.ids = ids;
        this.states = new HashSet<>();
    }

    @Override
    public Codec<ParticleExpireIfNotInBlocks> codec() {
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
        return "ParticleExpireIfNotInBlocks[" +
                "blocks=" + ids + ']';
    }

}

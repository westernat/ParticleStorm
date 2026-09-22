package org.mesdag.particlestorm.data.component;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.IParticleComponent;
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

    private static final Vector3f POSITION = new Vector3f();

    @Override
    public void apply(IMolangParticleInstance instance) {
        POSITION.set((float) instance.getX(), (float) instance.getY(), (float) instance.getZ());
        instance.getEmitter().local2World(POSITION, 1.0F);
        BlockState state = instance.getLevel().getBlockState(BlockPos.containing(POSITION.x, POSITION.y, POSITION.z));
        if (!states.contains(state)) {
            instance.discard();
        }
    }

    @Override
    public void update(IMolangParticleInstance instance) {
        apply(instance);
    }

    @Override
    public boolean requireUpdate() {
        return true;
    }

    @Override
    public String toString() {
        return "ParticleExpireIfNotInBlocks[" +
                "blocks=" + ids + ']';
    }
}

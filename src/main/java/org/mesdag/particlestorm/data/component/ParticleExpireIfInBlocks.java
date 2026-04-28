package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.IParticleComponent;
import org.mesdag.particlestorm.data.molang.MolangExp;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Particles expire when in a block of the type in the list.
///
/// Note: this component can exist alongside particle_lifetime_expression.
public final class ParticleExpireIfInBlocks implements IParticleComponent {
    public static final Codec<ParticleExpireIfInBlocks> CODEC = Codec.list(Codec.STRING).xmap(
            states -> new ParticleExpireIfInBlocks(new HashSet<>(states)),
            blocks -> List.copyOf(blocks.ids)
    );
    public final Set<String> ids;
    public final Set<Block> blocks;

    public ParticleExpireIfInBlocks(Set<String> ids) {
        this.ids = ids;
        this.blocks = new HashSet<>();
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
        if (blocks.isEmpty()) {
            for (String id : ids) {
                BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id)).ifPresent(blocks::add);
            }
        }
    }

    @Override
    public void apply(IMolangParticleInstance instance) {
        if (!blocks.contains(instance.getLevel().getBlockState(BlockPos.containing(instance.getPosition())).getBlock())) {
            instance.discard();
        }
    }

    @Override
    public String toString() {
        return "ParticleExpireIfInBlocks[blocks=" + ids + ']';
    }
}

package org.mesdag.particlestorm.data.component;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;
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
    public final Set<Block> blocks;

    public ParticleExpireIfNotInBlocks(Set<String> ids) {
        this.ids = ids;
        this.blocks = new HashSet<>();
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
        if (blocks.isEmpty()) {
            for (String id : ids) {
                Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(id));
                if (block != null) {
                    blocks.add(block);
                }
            }
        }
    }

    private static final Vector3f vector3f = new Vector3f();

    @Override
    public void apply(IMolangParticleInstance instance) {
        instance.getEmitter().local2World(vector3f.set((float) instance.getX(), (float) instance.getY(), (float) instance.getZ()), 1);
        if (!blocks.contains(instance.getLevel().getBlockState(BlockPos.containing(vector3f.x, vector3f.y, vector3f.z)).getBlock())) {
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
        return "ParticleExpireIfNotInBlocks[blocks=" + ids + ']';
    }
}

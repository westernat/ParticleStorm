package org.mesdag.particlestorm.data.molang.compiler.function.misc;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.function.MathFunction;
import org.mesdag.particlestorm.data.molang.compiler.value.StringValue;

public final class IsBlockFunction extends MathFunction {
    private final StringValue stringValue;
    private final Either<Block, TagKey<Block>> either;

    public IsBlockFunction(MathValue... values) {
        super(values);
        if (values[0] instanceof StringValue stringValue) {
            this.stringValue = stringValue;
            String value = stringValue.value();
            this.either = value.startsWith("#")
                    ? Either.right(BlockTags.create(ResourceLocation.parse(value.substring(1))))
                    : Either.left(ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(value)));
        } else {
            throw new IllegalArgumentException(values[0] + " is not a string value");
        }
    }

    @Override
    public String getName() {
        return "query.is_block";
    }

    @Override
    public float compute(MolangInstance instance) {
        BlockState state = instance.getLevel().getBlockState(BlockPos.containing(instance.getPosition()));
        return either.map(state::is, state::is) ? 1 : 0;
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public MathValue[] getArgs() {
        return new MathValue[]{stringValue};
    }
}

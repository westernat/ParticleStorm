package org.mesdag.particlestorm.data.molang.compiler.function.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.function.MathFunction;
import org.mesdag.particlestorm.data.molang.compiler.value.StringValue;

import java.util.Optional;

public final class IsBlockFunction extends MathFunction {
    private final StringValue stringValue;
    private final Optional<Block> block;
    private final Optional<TagKey<Block>> tag;

    public IsBlockFunction(MathValue... values) {
        super(values);
        if (values[0] instanceof StringValue stringValue) {
            this.stringValue = stringValue;
            String value = stringValue.value();
            if (value.startsWith("#")) {
                this.block = Optional.empty();
                this.tag = Optional.of(TagKey.create(Registries.BLOCK, Identifier.parse(value.substring(1))));
            } else {
                this.block = BuiltInRegistries.BLOCK.get(Identifier.parse(value)).map(holder -> holder.value());
                this.tag = Optional.empty();
            }
        } else {
            throw new IllegalArgumentException(values[0] + " is not a string value");
        }
    }

    @Override
    public String getName() {
        return "query.is_block";
    }

    @Override
    public double compute(MolangInstance instance) {
        BlockState state = instance.getLevel().getBlockState(BlockPos.containing(instance.getPosition()));
        if (tag.isPresent()) {
            return state.is(tag.get()) ? 1.0 : 0.0;
        }
        return block.map(state::is).orElse(false) ? 1.0 : 0.0;
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

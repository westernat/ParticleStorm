package org.mesdag.particlestorm.data.molang;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.mesdag.particlestorm.data.molang.compiler.MathParser;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;

public class MolangExp {
    public static final MolangExp EMPTY = new MolangExp("");
    public static final Codec<MolangExp> CODEC = Codec.STRING.xmap(MolangExp::new, e -> e.expStr);
    public static final StreamCodec<ByteBuf, MolangExp> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, e -> e.expStr,
            MolangExp::new
    );
    protected final String expStr;
    protected MathValue variable;

    public MolangExp(String expStr) {
        this.expStr = expStr;
    }

    public String getExpStr() {
        return expStr;
    }

    public void compile(MathParser parser) {
        if (variable == null && !expStr.isEmpty() && !expStr.isBlank()) {
            this.variable = parser.compileMolang(expStr);
        }
    }

    public float calculate(MolangInstance instance) {
        if (!initialized()) return 0.0F;
        return (float) variable.get(instance);
    }

    public MathValue getVariable() {
        return variable;
    }

    public boolean initialized() {
        return variable != null;
    }

    @Override
    public String toString() {
        return "MolangExp{" + expStr + "}";
    }
}

package org.mesdag.particlestorm.data.molang.compiler.function.random;

import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.function.MathFunction;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/// [MathFunction] value supplier
///
/// **Contract:**
///
/// Returns a random value based on the input values:
///
///   - Three inputs generates the sum of _n_ (first input) random values between the second (inclusive) and third input (exclusive)
///   - Four inputs generates the sum of _n_ (first input) random values between the second (inclusive) and third input (exclusive), seeded by the fourth input
///
public final class DieRollFunction extends MathFunction {
    private final MathValue rolls;
    private final MathValue min;
    private final MathValue max;
    @Nullable
    private final MathValue seed;
    @Nullable
    private final Random random;

    public DieRollFunction(MathValue... values) {
        super(values);

        this.rolls = values[0];
        this.min = values[1];
        this.max = values[2];
        this.seed = values.length >= 4 ? values[3] : null;
        this.random = this.seed != null ? new Random() : null;
    }

    @Override
    public String getName() {
        return "math.die_roll";
    }

    @Override
    public float compute(MolangInstance instance) {
        int rolls = (int) (Math.floor(this.rolls.get(instance)));
        float min = this.min.get(instance);
        float max = this.max.get(instance);
        float sum = 0;
        Random random;

        if (this.random != null) {
            random = this.random;
            random.setSeed((long) this.seed.get(instance));
        } else {
            random = ThreadLocalRandom.current();
        }

        for (int i = 0; i < rolls; i++) {
            sum += min + random.nextFloat() * (max - min);
        }

        return sum;
    }

    @Override
    public boolean isMutable(MathValue... values) {
        if (values.length < 4)
            return true;

        return super.isMutable(values);
    }

    @Override
    public int getMinArgs() {
        return 3;
    }

    @Override
    public MathValue[] getArgs() {
        if (this.seed != null)
            return new MathValue[]{this.rolls, this.min, this.max, this.seed};

        return new MathValue[]{this.rolls, this.min, this.max};
    }
}

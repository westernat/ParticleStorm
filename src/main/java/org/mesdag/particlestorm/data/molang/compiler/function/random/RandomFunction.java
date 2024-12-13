package org.mesdag.particlestorm.data.molang.compiler.function.random;

import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.function.MathFunction;

import java.util.Random;

/**
 * {@link MathFunction} value supplier
 *
 * <p>
 * <b>Contract:</b>
 * <br>
 * Returns a random value based on the input values:
 * <ul>
 *     <li>A single input generates a value between 0 and that input (exclusive)</li>
 *     <li>Two inputs generates a random value between the first (inclusive) and second input (exclusive)</li>
 *     <li>Three inputs generates a random value between the first (inclusive) and second input (exclusive), seeded by the third input</li>
 * </ul>
 */
public final class RandomFunction extends MathFunction {
    private final MathValue valueA;
    @Nullable
    private final MathValue valueB;
    @Nullable
    private final MathValue seed;
    @Nullable
    private final Random random;

    public RandomFunction(MathValue... values) {
        super(values);

        this.valueA = values[0];
        this.valueB = values.length >= 2 ? values[1] : null;
        this.seed = values.length >= 3 ? values[2] : null;
        this.random = this.seed != null ? new Random() : null;
    }

    @Override
    public String getName() {
        return "math.random";
    }

    @Override
    public double compute(MolangInstance instance) {
        double result;
        double valueA = this.valueA.get(instance);

        if (this.random != null) {
            this.random.setSeed((long)this.seed.get(instance));

            result = this.random.nextDouble();
        }
        else {
            result = Math.random();
        }

        if (this.valueB != null) {
            double valueB = this.valueB.get(instance);
            double min = Math.min(valueA, valueB);
            double max = Math.max(valueA, valueB);

            result = min + result * (max - min);
        }
        else {
            result = result * valueA;
        }

        return result;
    }

    @Override
    public boolean isMutable(MathValue... values) {
        if (values.length < 3)
            return true;

        return super.isMutable(values);
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public MathValue[] getArgs() {
        if (this.seed != null)
            return new MathValue[] {this.valueA, this.valueB, this.seed};

        if (this.valueB != null)
            return new MathValue[] {this.valueA, this.valueB};

        return new MathValue[] {this.valueA};
    }
}

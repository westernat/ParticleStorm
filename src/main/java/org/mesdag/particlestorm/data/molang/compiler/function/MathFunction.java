package org.mesdag.particlestorm.data.molang.compiler.function;

import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;

import java.util.StringJoiner;

/// Computational function wrapping a [MathValue]
///
/// Subclasses of this represent mathematical functions to be performed on a pre-defined number of input variables.
///
/// Functions should be deterministic - identical input values should result in identical output values, and values should adhere to the [Mutability][#isMutable()] contract of [MathValue] for determining result-caching.
public abstract class MathFunction implements MathValue {
    private boolean isMutable;
    private float cachedValue = Float.MIN_VALUE;

    protected MathFunction(MathValue... values) {
        validate(values);

        this.isMutable = isMutable(values);
    }

    /// Return the expression name/symbol for this function.
    /// This is the value that would be seen in a mathematical expression string
    public abstract String getName();

    @Override
    public final float get(MolangInstance instance) {
        if (this.isMutable)
            return compute(instance);

        if (this.cachedValue == Float.MIN_VALUE)
            this.cachedValue = compute(instance);

        return this.cachedValue;
    }

    /// Compute the result of this function from its stored arguments
    public abstract float compute(MolangInstance instance);

    /// @return Whether this function should be considered mutable; the value could change
    ///
    /// This would normally be determined by whether any of the stored args are mutable
    public boolean isMutable(MathValue... values) {
        for (MathValue value : values) {
            if (value.isMutable())
                return true;
        }

        return false;
    }

    @Override
    public void markImmutable() {
        this.isMutable = false;
    }

    /// @return The minimum number of args required for this function to be computable
    public abstract int getMinArgs();

    /// @return The arguments (in order) stored in this function
    public abstract MathValue[] getArgs();

    /// Validate this function's arguments against its minimum requirements, throwing an exception for invalid argument states
    public void validate(MathValue... inputs) throws IllegalArgumentException {
        final int minArgs = getMinArgs();

        if (inputs.length < minArgs)
            throw new IllegalArgumentException(String.format("Function '%s' at least %s arguments. Only %s given!", getName(), minArgs, inputs.length));
    }

    @Override
    public final boolean isMutable() {
        return this.isMutable;
    }

    @Override
    public String toString() {
        final MathValue[] args = getArgs();
        final StringJoiner joiner = new StringJoiner(", ", "(", ")");

        for (MathValue arg : args) {
            joiner.add(arg.toString());
        }

        return getName() + joiner;
    }

    /// Factory interface for [MathFunction].
    /// Functionally equivalent to <pre>`Function<MathValue[], MathFunction>`</pre> but with a more concise user-facing handle
    @FunctionalInterface
    public interface Factory<T extends MathFunction> {
        /// Instantiate a new [MathFunction] for the given input values
        T create(MathValue... values);
    }
}

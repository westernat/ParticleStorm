package org.mesdag.particlestorm.data.molang.compiler;

import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.data.molang.compiler.function.MathFunction;
import org.mesdag.particlestorm.data.molang.compiler.function.generic.*;
import org.mesdag.particlestorm.data.molang.compiler.function.limit.ClampFunction;
import org.mesdag.particlestorm.data.molang.compiler.function.limit.MaxFunction;
import org.mesdag.particlestorm.data.molang.compiler.function.limit.MinFunction;
import org.mesdag.particlestorm.data.molang.compiler.function.misc.PiFunction;
import org.mesdag.particlestorm.data.molang.compiler.function.misc.ToDegFunction;
import org.mesdag.particlestorm.data.molang.compiler.function.misc.ToRadFunction;
import org.mesdag.particlestorm.data.molang.compiler.function.random.DieRollFunction;
import org.mesdag.particlestorm.data.molang.compiler.function.random.DieRollIntegerFunction;
import org.mesdag.particlestorm.data.molang.compiler.function.random.RandomFunction;
import org.mesdag.particlestorm.data.molang.compiler.function.random.RandomIntegerFunction;
import org.mesdag.particlestorm.data.molang.compiler.function.round.*;
import org.mesdag.particlestorm.data.molang.compiler.value.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static org.mesdag.particlestorm.data.molang.compiler.MolangQueries.applyPrefixAliases;

public class MolangParser {
    private static final Pattern EXPRESSION_FORMAT = Pattern.compile("^[\\w\\s_+-/*%^&|<>=!?:.,()]+$");
    private static final Pattern WHITESPACE = Pattern.compile("\\s");
    private static final Pattern NUMERIC = Pattern.compile("^-?\\d+(\\.\\d+)?$");
    private static final String MOLANG_RETURN = "return ";
    private static final String STATEMENT_DELIMITER = ";";
    private static final Map<String, MathFunction.Factory<?>> FUNCTION_FACTORIES = Util.make(new ConcurrentHashMap<>(18), map -> {
        map.put("math.abs", AbsFunction::new);
        map.put("math.acos", ACosFunction::new);
        map.put("math.asin", ASinFunction::new);
        map.put("math.atan", ATanFunction::new);
        map.put("math.atan2", ATan2Function::new);
        map.put("math.ceil", CeilFunction::new);
        map.put("math.clamp", ClampFunction::new);
        map.put("math.cos", CosFunction::new);
        map.put("math.die_roll", DieRollFunction::new);
        map.put("math.die_roll_integer", DieRollIntegerFunction::new);
        map.put("math.exp", ExpFunction::new);
        map.put("math.floor", FloorFunction::new);
        map.put("math.lerprotate", LerpRotFunction::new);
        map.put("math.hermite_blend", HermiteBlendFunction::new);
        map.put("math.lerp", LerpFunction::new);
        map.put("math.ln", LogFunction::new);
        map.put("math.max", MaxFunction::new);
        map.put("math.min", MinFunction::new);
        map.put("math.mod", ModFunction::new);
        map.put("math.pi", PiFunction::new);
        map.put("math.pow", PowFunction::new);
        map.put("math.random", RandomFunction::new);
        map.put("math.random_integer", RandomIntegerFunction::new);
        map.put("math.round", RoundFunction::new);
        map.put("math.sin", SinFunction::new);
        map.put("math.sqrt", SqrtFunction::new);
        map.put("math.to_deg", ToDegFunction::new);
        map.put("math.to_rad", ToRadFunction::new);
        map.put("math.trunc", TruncateFunction::new);
    });
    private final VariableTable table;

    public MolangParser(VariableTable table) {
        this.table = table;
    }

    public boolean isFunctionRegistered(String name) {
        return FUNCTION_FACTORIES.containsKey(name);
    }

    @Nullable
    public <T extends MathFunction> T buildFunction(String name, MathValue... values) {
        if (!FUNCTION_FACTORIES.containsKey(name))
            return null;

        return (T) FUNCTION_FACTORIES.get(name).create(values);
    }

    public Variable getVariableFor(String name) {
        if (name.startsWith("q")) {
            return MolangQueries.getQueryFor(name);
        }
        String n = applyPrefixAliases(name, "variable.", "v.");
        return table.computeIfAbsent(n, s -> new Variable(s, p -> p.getVars().getValue(s, p)));
    }

    public MathValue compileMolang(String expression) {
        if (expression.startsWith(MOLANG_RETURN)) {
            expression = expression.substring(MOLANG_RETURN.length());

            if (expression.contains(STATEMENT_DELIMITER))
                expression = expression.substring(0, expression.indexOf(STATEMENT_DELIMITER));
        } else if (expression.contains(STATEMENT_DELIMITER)) {
            final String[] subExpressions = expression.split(STATEMENT_DELIMITER);
            final List<MathValue> subValues = new ObjectArrayList<>(subExpressions.length);

            for (String subExpression : subExpressions) {
                boolean isReturn = subExpression.startsWith(MOLANG_RETURN);

                if (isReturn)
                    subExpression = subExpression.substring(MOLANG_RETURN.length());

                subValues.add(compileExpression(subExpression));

                if (isReturn)
                    break;
            }

            return new CompoundValue(subValues.toArray(new MathValue[0]));
        }

        return compileExpression(expression);
    }

    public MathValue compileExpression(String expression) {
        return parseSymbols(compileSymbols(decomposeExpression(expression)));
    }

    public char[] decomposeExpression(String expression) throws IllegalArgumentException {
        if (!EXPRESSION_FORMAT.matcher(expression).matches())
            throw new IllegalArgumentException("Invalid characters found in expression: '" + expression + "'");

        final char[] chars = WHITESPACE.matcher(expression).replaceAll("").toLowerCase(Locale.ROOT).toCharArray();
        int groupState = 0;

        for (char character : chars) {
            if (character == '(') {
                groupState++;
            } else if (character == ')') {
                groupState--;
            }

            if (groupState < 0)
                throw new IllegalArgumentException("Closing parenthesis before opening parenthesis in expression '" + expression + "'");
        }

        if (groupState != 0)
            throw new IllegalArgumentException("Uneven parenthesis in expression, each opening brace must have a pairing close brace '" + expression + "'");

        return chars;
    }

    @Nullable
    protected String tryMergeOperativeSymbols(char[] chars, int index) {
        char ch = chars[index];

        if (!Operator.isOperativeSymbol(ch))
            return null;

        int maxLength = Math.min(chars.length - index, Operator.maxOperatorLength());

        for (int length = maxLength; length > 0; length--) {
            String testOperator = String.copyValueOf(chars, index, length);

            if (Operator.isOperator(testOperator))
                return testOperator;
        }

        if (ch == '?' || ch == ':' || ch == ',')
            return String.valueOf(ch);

        return null;
    }

    public List<Either<String, List<MathValue>>> compileSymbols(char[] chars) {
        final List<Either<String, List<MathValue>>> symbols = new ObjectArrayList<>();
        final StringBuilder buffer = new StringBuilder();
        int lastSymbolIndex = -1;

        for (int i = 0; i < chars.length; i++) {
            final char ch = chars[i];

            if (ch == '-' && buffer.isEmpty() && (symbols.isEmpty() || lastSymbolIndex == symbols.size() - 1)) {
                buffer.append(ch);

                continue;
            }

            final String operator = tryMergeOperativeSymbols(chars, i);

            if (operator != null) {
                i += operator.length() - 1;

                if (!buffer.isEmpty())
                    symbols.add(Either.left(buffer.toString()));

                lastSymbolIndex = symbols.size();

                symbols.add(Either.left(operator));
                buffer.setLength(0);
            } else if (ch == '(') {
                if (!buffer.isEmpty()) {
                    symbols.add(Either.left(buffer.toString()));
                    buffer.setLength(0);
                }

                List<MathValue> subValues = new ObjectArrayList<>();
                int groupState = 1;

                for (int j = i + 1; j < chars.length; j++) {
                    final char groupChar = chars[j];

                    if (groupChar == '(') {
                        groupState++;
                    } else if (groupChar == ')') {
                        groupState--;
                    } else if (groupChar == ',' && groupState == 1) {
                        subValues.add(parseSymbols(compileSymbols(buffer.toString().toCharArray())));
                        buffer.setLength(0);

                        continue;
                    }

                    if (groupState == 0) {
                        if (!buffer.isEmpty())
                            subValues.add(parseSymbols(compileSymbols(buffer.toString().toCharArray())));

                        i = j;

                        symbols.add(Either.right(subValues));
                        buffer.setLength(0);

                        break;
                    } else {
                        buffer.append(groupChar);
                    }
                }
            } else {
                buffer.append(ch);
            }
        }

        if (!buffer.isEmpty())
            symbols.add(Either.left(buffer.toString()));

        return symbols;
    }

    public MathValue parseSymbols(List<Either<String, List<MathValue>>> symbols) throws IllegalArgumentException {
        if (symbols.size() == 2) {
            Optional<String> prefix = symbols.getFirst().left().filter(left -> left.startsWith("-") || left.startsWith("!") || isFunctionRegistered(left));
            Optional<List<MathValue>> group = symbols.get(1).right();

            if (prefix.isPresent() && group.isPresent())
                return compileFunction(prefix.get(), group.get());
        }

        MathValue value = compileValue(symbols);

        if (value != null)
            return value;

        throw new IllegalArgumentException("Unable to parse compiled symbols from expression: " + symbols);
    }

    @Nullable
    protected MathValue compileValue(List<Either<String, List<MathValue>>> symbols) throws IllegalArgumentException {
        if (symbols.size() == 1)
            return compileSingleValue(symbols.getFirst());

        Ternary ternary = compileTernary(symbols);

        if (ternary != null)
            return ternary;

        return compileCalculation(symbols);
    }

    @Nullable
    protected MathValue compileSingleValue(Either<String, List<MathValue>> symbol) throws IllegalArgumentException {
        if (symbol.right().isPresent())
            return new Group(symbol.right().get().getFirst());

        return symbol.left().map(string -> {
            if (string.startsWith("!"))
                return new BooleanNegate(compileSingleValue(Either.left(string.substring(1))));

            if (isNumeric(string))
                return new Constant(Double.parseDouble(string));

            if (isLikelyVariable(string)) {
                if (string.startsWith("-"))
                    return new Negative(getVariableFor(string.substring(1)));

                return getVariableFor(string);
            }

            if (isFunctionRegistered(string))
                return compileFunction(string, List.of());

            return null;
        }).orElse(null);
    }

    @Nullable
    protected MathValue compileCalculation(List<Either<String, List<MathValue>>> symbols) throws IllegalArgumentException {
        final int symbolCount = symbols.size();
        int operatorIndex = -1;
        Operator lastOperator = null;

        for (int i = 1; i < symbolCount; i++) {
            Operator operator = symbols.get(i).left()
                    .filter(Operator::isOperator)
                    .map(MolangParser::getOperatorFor).orElse(null);

            if (operator == null)
                continue;

            if (operator == Operator.ASSIGN_VARIABLE) {
                if (!(parseSymbols(symbols.subList(0, i)) instanceof Variable v))
                    throw new IllegalArgumentException("Attempted to assign a value to a non-variable");

                return new VariableAssignment(v, parseSymbols(symbols.subList(i + 1, symbolCount)));
            }

            if (lastOperator == null || !operator.takesPrecedenceOver(lastOperator)) {
                operatorIndex = i;
                lastOperator = operator;
            } else {
                break;
            }
        }

        return lastOperator == null ? null : new Calculation(lastOperator, parseSymbols(symbols.subList(0, operatorIndex)), parseSymbols(symbols.subList(operatorIndex + 1, symbolCount)));
    }

    @Nullable
    protected Ternary compileTernary(List<Either<String, List<MathValue>>> symbols) throws IllegalArgumentException {
        final int symbolCount = symbols.size();

        if (symbolCount < 3)
            return null;

        Supplier<MathValue> condition = null;
        Supplier<MathValue> ifTrue = null;
        int ternaryState = 0;
        int lastColon = -1;
        int queryIndex = -1;

        for (int i = 0; i < symbolCount; i++) {
            final int i2 = i;
            final String string = symbols.get(i).left().orElse(null);

            if ("?".equals(string)) {
                if (condition == null) {
                    condition = () -> parseSymbols(symbols.subList(0, i2));
                    queryIndex = i2 + 1;
                }

                ternaryState++;
            } else if (":".equals(string)) {
                if (ternaryState == 1 && ifTrue == null && queryIndex > 0) {
                    final int queryIndex2 = queryIndex;
                    ifTrue = () -> parseSymbols(symbols.subList(queryIndex2, i2));
                }

                ternaryState--;
                lastColon = i;
            }
        }

        if (ternaryState == 0 && condition != null && ifTrue != null && lastColon < symbolCount - 1)
            return new Ternary(condition.get(), ifTrue.get(), parseSymbols(symbols.subList(lastColon + 1, symbolCount)));

        return null;
    }

    @Nullable
    protected MathValue compileFunction(String name, List<MathValue> args) throws IllegalArgumentException {
        if (name.startsWith("!")) {
            if (name.length() == 1)
                return new BooleanNegate(args.getFirst());

            return new BooleanNegate(compileFunction(name.substring(1), args));
        }

        if (name.startsWith("-")) {
            if (name.length() == 1)
                return new Negative(args.getFirst());

            return new Negative(compileFunction(name.substring(1), args));
        }

        if (!isFunctionRegistered(name))
            return null;

        return buildFunction(name, args.toArray(new MathValue[0]));
    }

    @Deprecated(forRemoval = true)
    public static boolean isOperativeSymbol(char symbol) {
        return isOperativeSymbol(String.valueOf(symbol));
    }

    @Deprecated(forRemoval = true)
    public static boolean isOperativeSymbol(@NotNull String symbol) {
        return Operator.isOperator(symbol) || symbol.equals("?") || symbol.equals(":");
    }

    public static boolean isNumeric(String string) {
        return NUMERIC.matcher(string).matches();
    }

    protected static Operator getOperatorFor(String op) throws IllegalArgumentException {
        return Operator.getOperatorFor(op).orElseThrow(() -> new IllegalArgumentException("Unknown operator symbol '" + op + "'"));
    }

    @Deprecated(forRemoval = true)
    protected static boolean isQueryOrFunctionName(String string) {
        return !isNumeric(string) && !isOperativeSymbol(string);
    }

    protected boolean isLikelyVariable(String string) {
        if (MolangQueries.isExistingVariable(string))
            return true;

        return !isNumeric(string) && !isFunctionRegistered(string) && !Operator.isOperator(string) && !string.equals("?") && !string.equals(":");
    }
}

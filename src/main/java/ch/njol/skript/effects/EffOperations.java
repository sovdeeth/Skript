package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Patterns;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;
import org.skriptlang.skript.lang.arithmetic.Operation;
import org.skriptlang.skript.lang.arithmetic.Operator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Name("Operations")
@Description("Perform multiplcation, division, or exponentiation operations on variable numbers. Cannot use literals.")
@Example("""
	set {_num} to 1
	multiply {_num} by 10
	""")
@Example("""
	set {_nums::*} to 15, 21 and 30
	divide {_nums::*} by 3
	""")
@Example("""
	set {_num} to 2
	raise {_num} to the power of 5
	""")
@Example("""
	# Will error due to literal
	multiply 1 by 2
	""")
@Since("INSERT VERSION")
public class EffOperations extends Effect {

	private static final Patterns<Operator> patterns = new Patterns<>(new Object[][]{
		{"multiply %~numbers% by %number%", Operator.MULTIPLICATION},
		{"divide %~numbers% by %number%", Operator.DIVISION},
		{"raise %~numbers% to [the] (power|exponent) [of] %number%", Operator.EXPONENTIATION}
	});

	private static final Map<Operator, Operation<Number, Number, Number>> operations = new HashMap<>();

	static {
		Skript.registerEffect(EffOperations.class, patterns.getPatterns());
	}

	private Operator operator;
	private Expression<Number> baseExpr;
	private Expression<Number> operativeExpr;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		operator = patterns.getInfo(matchedPattern);
		//noinspection unchecked
		baseExpr = (Expression<Number>) exprs[0];
		operativeExpr = LiteralUtils.defendExpression(exprs[1]);
		return true;
	}

	@Override
	protected void execute(Event event) {
		Number operativeNumber = operativeExpr.getSingle(event);
		assert operativeNumber != null;
		Operation<Number, Number, Number> operation = getOperation(operator);
		assert operation != null;
		Function<Number, Number> function = number -> operation.calculate(number, operativeNumber);
		baseExpr.changeInPlace(event, function);
	}

	private static Operation<Number, Number, Number> getOperation(Operator operator) {
		if (operations.containsKey(operator))
			return operations.get(operator);
		Operation<Number, Number, Number> operation = Arithmetics.getOperation(operator, Number.class, Number.class, Number.class);
		if (operation != null) {
			operations.put(operator, operation);
			return operation;
		}
		throw new IllegalStateException("No number operation for operator '" + operator + "'.");
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
		switch (operator) {
			case MULTIPLICATION -> builder.append("multiply", baseExpr, "by");
			case DIVISION -> builder.append("divide", baseExpr, "by");
			case EXPONENTIATION -> builder.append("raise", baseExpr, "to the power of");
		}
		builder.append(operativeExpr);
		return builder.toString();
	}

}

package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Node;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Patterns;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;
import org.skriptlang.skript.lang.arithmetic.Operation;
import org.skriptlang.skript.lang.arithmetic.Operator;
import org.skriptlang.skript.log.runtime.SyntaxRuntimeErrorProducer;

import java.util.function.Function;

@Name("Operations")
@Description("Perform multiplication, division, or exponentiation operations on variable objects " +
	"(i.e. numbers, vectors, timespans, and other objects from addons). Cannot use literals.")
@Example("""
	set {_num} to 1
	multiply {_num} by 10
	divide {_num} by 5
	raise {_num} to the power of 2
	""")
@Example("""
	set {_nums::*} to 15, 21 and 30
	divide {_nums::*} by 3
	multiply {_nums::*} by 5
	raise {_nums::*} to the power of 3
	""")
@Example("""
	set {_vector} to vector(1,1,1)
	multiply {_vector} by vector(4,8,16)
	divide {_vector} by 2
	""")
@Example("""
	set {_timespan} to 1 hour
	multiply {_timespan} by 3
	""")
@Example("""
	# Will error due to literal
	multiply 1 by 2
	""")
@Since("INSERT VERSION")
public class EffOperations extends Effect implements SyntaxRuntimeErrorProducer {

	private static final Patterns<Operator> patterns = new Patterns<>(new Object[][]{
		{"multiply %~objects% by %object%", Operator.MULTIPLICATION},
		{"divide %~objects% by %object%", Operator.DIVISION},
		{"raise %~objects% to [the] (power|exponent) [of] %object%", Operator.EXPONENTIATION}
	});

	static {
		Skript.registerEffect(EffOperations.class, patterns.getPatterns());
	}

	private Operator operator;
	private Expression<?> base;
	private Expression<?> operative;
	private Node node;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		operator = patterns.getInfo(matchedPattern);
		base = exprs[0];
		if (!(base instanceof Variable<?>)) {
			Skript.error("Cannot operate on a non variable object.");
			return false;
		}
		operative = LiteralUtils.defendExpression(exprs[1]);
		node = getParser().getNode();
		return LiteralUtils.canInitSafely(operative);
	}

	@Override
	protected void execute(Event event) {
		Object operativeObject = operative.getSingle(event);
		if (operativeObject == null)
			return;
		ClassInfo<?> operativeClassInfo = Classes.getSuperClassInfo(operativeObject.getClass());
		Class<?> operativeClass = operativeClassInfo.getC();
		Operation<Object, Object, Object> operation = null;
		Function<?, Object> function = null;
		if (base.isSingle()) {
			// If the variable provided is single, then we can do some checks
			// to ensure an operation is available; if not, we can produce a proper error.
			Object baseObject = base.getSingle(event);
			if (baseObject == null)
				return;
			ClassInfo<?> baseClassInfo = Classes.getSuperClassInfo(baseObject.getClass());
			Class<?> baseClass = baseClassInfo.getC();
			if (!baseClass.equals(operativeClass)) {
				operation = getOperation(baseClass, operativeClass);
				if (operation == null) {
					error(Utils.A(baseClassInfo.getCodeName()) + "cannot be " + getOperatorString()
							+ " with " + Utils.a(operativeClassInfo.getCodeName()));
					return;
				}
			} else {
				operation = getOperation(operativeClass);
				if (operation == null) {
					error(Utils.A(operativeClassInfo.getCodeName()) + " cannot be " + getOperatorString() + ".");
					return;
				}
			}
			Operation<Object, Object, Object> finalOperation = operation;
			function = object -> finalOperation.calculate(object, operativeObject);
		} else {
			// In the case of a list variable, we should probably ignore throwing multiple errors for each object
			// that is not applicable for an operation
			function = object -> {
				Operation<Object, Object, Object> thisOperation = getOperation(object.getClass(), operativeClass);
				if (thisOperation != null)
					return thisOperation.calculate(object, operativeObject);
				return object;
			};
		}
		//noinspection unchecked,rawtypes
		base.changeInPlace(event, (Function) function);
	}

	private @Nullable Operation<Object, Object, Object> getOperation(Class<?> type) {
		//noinspection unchecked
		return (Operation<Object, Object, Object>) Arithmetics.getConvertedOperation(operator, type, type, type);
	}

	private @Nullable Operation<Object, Object, Object> getOperation(Class<?> leftClass, Class<?> rightClass) {
		//noinspection unchecked
		return (Operation<Object, Object, Object>) Arithmetics.getConvertedOperation(operator, leftClass, rightClass, leftClass);
	}

	@Override
	public Node getNode() {
		return node;
	}

	private String getOperatorString() {
		return switch (operator) {
			case MULTIPLICATION -> "multiplied";
			case DIVISION -> "divided";
			case EXPONENTIATION -> "exponentiated";
			default -> "";
		};
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
		switch (operator) {
			case MULTIPLICATION -> builder.append("multiply", base, "by");
			case DIVISION -> builder.append("divide", base, "by");
			case EXPONENTIATION -> builder.append("raise", base, "to the power of");
		}
		builder.append(operative);
		return builder.toString();
	}

}

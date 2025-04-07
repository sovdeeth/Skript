package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.Changer.ChangerUtils;
import ch.njol.skript.config.Node;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.arithmetic.ExprArithmetic;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Patterns;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;
import org.skriptlang.skript.lang.arithmetic.Operation;
import org.skriptlang.skript.lang.arithmetic.OperationInfo;
import org.skriptlang.skript.lang.arithmetic.Operator;
import org.skriptlang.skript.log.runtime.SyntaxRuntimeErrorProducer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Name("Operations")
@Description("Perform multiplication, division, or exponentiation operations on variable objects "
	+ "(i.e. numbers, vectors, timespans, and other objects from addons). "
	+ "Literals cannot be used on the left-hand side.")
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
	divide 10 by {_num}
	""")
@Since("INSERT VERSION")
public class EffOperations extends Effect implements SyntaxRuntimeErrorProducer {

	private static final Patterns<Operator> PATTERNS = new Patterns<>(new Object[][]{
		{"multiply %~objects% by %object%", Operator.MULTIPLICATION},
		{"divide %~objects% by %object%", Operator.DIVISION},
		{"raise %~objects% to [the] (power|exponent) [of] %object%", Operator.EXPONENTIATION}
	});

	static {
		Skript.registerEffect(EffOperations.class, PATTERNS.getPatterns());
	}

	private Operator operator;
	private Expression<?> left;
	private Class<?>[] leftAccepts;
	private Expression<?> right;
	private Node node;
	OperationInfo<?, ?, ?> operationInfo;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		operator = PATTERNS.getInfo(matchedPattern);
		node = getParser().getNode();
		left = exprs[0];
		right = LiteralUtils.defendExpression(exprs[1]);

		// check if left accepts change
		leftAccepts = left.acceptChange(ChangeMode.SET);
		if (leftAccepts == null) {
			Skript.error("'" + left + "' cannot be set to anything and therefore cannot be " + getOperatedName());
			return false;
		} else if (leftAccepts.length == 0) {
			throw new IllegalStateException("An expression should never return an empty array for a ChangeMode of 'SET'");
		}
		// unwrap array types
		for (int i = 0; i < leftAccepts.length; i++) {
			if (leftAccepts[i].isArray()) {
				leftAccepts[i] = leftAccepts[i].getComponentType();
			}
		}

		// get return types
		Class<?> rightType = right.getReturnType();
		Class<?> leftType = left.getReturnType();

		// Ensure 'baseType' is referencing a non-array class
		if (leftType.isArray())
			leftType = leftType.getComponentType();

		// attempt to verify an operation exists
		if (leftType.equals(Object.class) && rightType.equals(Object.class)) {
			// Both are object, so we should check to see if '#getAllReturnTypes' contains a class
			// that is accepted within 'baseAccepted'
			Class<?>[] allReturnTypes = Arithmetics.getAllReturnTypes(operator).toArray(Class[]::new);
			if (!ChangerUtils.acceptsChangeTypes(leftAccepts, allReturnTypes)) {
				Skript.error(left + " cannot be " + getOperatedName() + ".");
				return false;
			}
			return LiteralUtils.canInitSafely(left);
		} else if (leftType.equals(Object.class) || rightType.equals(Object.class)) {
			// One is 'Object', so we can at least see if the other has any operations registered
			Class<?>[] returnTypes;
			if (leftType.equals(Object.class)) {
				// left is object, find all operations that match the right type
				returnTypes = Arithmetics.getOperations(operator).stream()
					.filter(info -> info.getRight().isAssignableFrom(rightType))
					.map(OperationInfo::getReturnType)
					.toArray(Class[]::new);
			} else {
				// right is object, find all operations that match the left type
				returnTypes = Arithmetics.getOperations(operator, leftType).stream()
					.map(OperationInfo::getReturnType)
					.toArray(Class[]::new);
			}
			if (returnTypes.length == 0) {
				// sticking to ExprArithmetic errors when possible, up for debate.
				noOperationError(left, leftType, rightType);
				return false;
			}
			// ensure we can set the expression to one of the return types
			if (!ChangerUtils.acceptsChangeTypes(leftAccepts, returnTypes)) {
				genericParseError(left, rightType);
				return false;
			}
		} else {
			// We've deduced that the return types from both 'base' and 'operative' are guaranteed not to be 'Object.class'
			// We can check to see if an operation exists, if not, parse error
			operationInfo = Arithmetics.lookupOperationInfo(operator, leftType, rightType, leftAccepts);
			if (operationInfo == null || !ChangerUtils.acceptsChangeTypes(leftAccepts, operationInfo.getReturnType())) {
				genericParseError(left, rightType);
				return false;
			}
		}
		return LiteralUtils.canInitSafely(left);
	}

	@Override
	protected void execute(Event event) {
		// Determine right type (cannot safely determine type of 'left' until we know if it is a single or array)
		Object rightObject = right.getSingle(event);
		if (rightObject == null)
			return;
		Class<?> rightType = rightObject.getClass();

		Map<Class<?>, Operation<Object, Object, ?>> cachedOperations = new HashMap<>();
		Set<Class<?>> invalidTypes = new HashSet<>();

		// change in place has to
		Function<?, ?> function = (leftInput) -> {
			Class<?> leftType = leftInput.getClass();
			// check if it's a valid type
			if (invalidTypes.contains(leftType)) {
				printArithmeticError(leftType, rightType);
				return leftInput;
			}
			// check if we have a cached operation
			Operation<Object, Object, ?> operation = cachedOperations.get(leftType);
			if (operation == null) {
				// if we don't have a cached operation, we need to get the operation info
				//noinspection unchecked
				OperationInfo<Object, Object, ?> operationInfo = (OperationInfo<Object, Object, ?>) Arithmetics.lookupOperationInfo(operator, leftType, rightType, leftAccepts);
				if (operationInfo == null) {
					printArithmeticError(leftType, rightType);
					invalidTypes.add(leftType);
					return leftInput;
				}
				// cache the operation
				operation = operationInfo.getOperation();
				cachedOperations.put(leftInput.getClass(), operation);
			}
			// return the result of the operation
			return operation.calculate(leftInput, rightObject);
		};
		//noinspection unchecked,rawtypes
		left.changeInPlace(event, (Function) function);
	}

	@Override
	public Node getNode() {
		return node;
	}

	private void printArithmeticError(Class<?> left, Class<?> right) {
		String error = ExprArithmetic.getArithmeticErrorMessage(operator, left, right);
		if (error != null)
			error(error);
	}

	private void genericParseError(Expression<?> leftExpr, Class<?> rightType) {
		Skript.error("'" + leftExpr + "' cannot be " + getOperatedName() + " by " + Classes.getSuperClassInfo(rightType).getName().withIndefiniteArticle());
	}

	private void noOperationError(Expression<?> leftExpr, Class<?> leftType, Class<?> rightType) {
		// try to get the error message from ExprArithmetic if possible
		String error = ExprArithmetic.getArithmeticErrorMessage(operator, leftType, rightType);
		if (error != null) {
			Skript.error(error);
		} else {
			genericParseError(leftExpr, rightType);
		}
	}

	private String getOperatedName() {
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
			case MULTIPLICATION -> builder.append("multiply", left, "by");
			case DIVISION -> builder.append("divide", left, "by");
			case EXPONENTIATION -> builder.append("raise", left, "to the power of");
		}
		builder.append(right);
		return builder.toString();
	}

}

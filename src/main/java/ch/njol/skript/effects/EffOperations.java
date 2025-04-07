package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.config.Node;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Patterns;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;
import org.skriptlang.skript.lang.arithmetic.Operation;
import org.skriptlang.skript.lang.arithmetic.OperationInfo;
import org.skriptlang.skript.lang.arithmetic.Operator;
import org.skriptlang.skript.log.runtime.SyntaxRuntimeErrorProducer;

import java.util.concurrent.atomic.AtomicBoolean;
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
	private Expression<?> base;
	private Expression<?> operative;
	private Node node;
	private Operation<Object, Object, Object> operation = null;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		operator = PATTERNS.getInfo(matchedPattern);
		base = exprs[0];
		Class<?>[] baseAccepted = base.acceptChange(ChangeMode.SET);
		if (baseAccepted == null) {
			printError(true,null, null);
			return false;
		} else if (baseAccepted.length == 0) {
			throw new IllegalStateException("An expression should never return an empty array for a ChangeMode of 'SET'.");
		}
		operative = LiteralUtils.defendExpression(exprs[1]);
		node = getParser().getNode();
		Class<?> operativeType = operative.getReturnType();
		Class<?> baseType = base.getReturnType();
		// Ensure 'baseType' is referencing a non-array class
		if (baseType.isArray())
			baseType = baseType.getComponentType();
		if (baseType.equals(Object.class) && operativeType.equals(Object.class)) {
			// Both are object, so we should check to see if '#getAllReturnTypes' contains a class
			// that is accepted within 'baseAccepted'
			Class<?>[] allReturnTypes = Arithmetics.getAllReturnTypes(operator).toArray(Class[]::new);
			if (!isOfType(baseAccepted, allReturnTypes)) {
				printError(true,null, null);
				return false;
			}
			return LiteralUtils.canInitSafely(operative);
		} else if (baseType.equals(Object.class) || operativeType.equals(Object.class)) {
			// One is 'Object', so we can at least see if the other has any operations registered
			Class<?>[] returnTypes = null;
			if (baseType.equals(Object.class)) {
				returnTypes = Arithmetics.getOperations(operator).stream()
					.filter(info -> info.getRight().isAssignableFrom(operativeType))
					.map(OperationInfo::getReturnType)
					.toArray(Class[]::new);
				if (returnTypes.length == 0) {
					printError(true,null, operativeType);
					return false;
				}
			} else {
				returnTypes = Arithmetics.getOperations(operator, baseType).stream()
					.map(OperationInfo::getReturnType)
					.toArray(Class[]::new);
				if (returnTypes.length == 0) {
					printError(true,null, null);
					return false;
				}
			}
			if (!isOfType(baseAccepted, returnTypes)) {
				printError(true, null, null);
				return false;
			}
		} else {
			// We've deduced that the return types from both 'base' and 'operative' are guaranteed not to be 'Object.class'
			// We can check to see if an operation exists, if not, parse error
			OperationInfo<Object, Object, Object> operationInfo = getOperationInfo(baseType, operativeType);
			if (operationInfo == null) {
				printError(true,null, operativeType);
				return false;
			}
			if (!isOfType(baseAccepted, operationInfo.getReturnType())) {
				printError(true,null, operativeType);
				return false;
			}
			operation = operationInfo.getOperation();
		}
		return LiteralUtils.canInitSafely(operative);
	}

	@Override
	protected void execute(Event event) {
		Object operativeObject = operative.getSingle(event);
		if (operativeObject == null)
			return;
		// Ensure 'operativeType' is not 'Object.class' by using '#getReturnType' or the class of the object
		Class<?> operativeType = operative.getReturnType().equals(Object.class) ? operativeObject.getClass() : operative.getReturnType();
		Operation<Object, Object, Object> operation = this.operation;
		Function<?, Object> changerFunction = null;
		Class<?> baseType = base.getReturnType();
		Class<?>[] baseAccepted = base.acceptChange(ChangeMode.SET);
		assert baseAccepted != null;
		// Convert array classes to singular, allowing for easier checks
		if (baseType.isArray())
			baseType = baseType.getComponentType();
		// If 'base' is single or the '#getReturnType' returns a non 'Object.class' we can use the same operation
		if (base.isSingle() || !baseType.equals(Object.class)) {
			if (operation == null) {
				Object baseObject = base.getSingle(event);
				if (baseObject == null)
					return;
				// If we reached here through 'base.isSingle()', need to ensure the class is not 'Object.class'
				baseType = baseType.equals(Object.class) ? baseObject.getClass() : baseType;
				OperationInfo<Object, Object, Object> operationInfo = getOperationInfo(baseType, operativeType);
				if (operationInfo == null || !isOfType(baseAccepted, operationInfo.getReturnType())) {
					printError(false, baseType, operativeType);
					return;
				}
				operation = operationInfo.getOperation();
			}
			Operation<Object, Object, Object> finalOperation = operation;
			changerFunction = object -> finalOperation.calculate(object, operativeObject);
		} else {
			AtomicBoolean errorPrinted = new AtomicBoolean(false);
			changerFunction = object -> {
				OperationInfo<Object, Object, Object> operationInfo = getOperationInfo(object.getClass(), operativeType);
				if (operationInfo != null) {
					if (isOfType(baseAccepted, operationInfo.getReturnType())) {
						return operationInfo.getOperation().calculate(object, operativeObject);
					} else if (!errorPrinted.get()) {
						errorPrinted.set(true);
						printError(false, null, operativeType);
					}
				}
				return object;
			};
		}
		//noinspection unchecked,rawtypes
		base.changeInPlace(event, (Function) changerFunction);
	}

	private @Nullable OperationInfo<Object, Object, Object> getOperationInfo(Class<?> leftClass, Class<?> rightClass) {
		//noinspection unchecked
		return (OperationInfo<Object, Object, Object>) Arithmetics.lookupOperationInfo(operator, leftClass, rightClass);
	}

	private boolean isOfType(Class<?>[] acceptedTypes, Class<?>[] checkTypes) {
		for (Class<?> check : checkTypes) {
			if (isOfType(acceptedTypes, check)) {
				return true;
			}
		}
		return false;
	}

	private boolean isOfType(Class<?>[] acceptedTypes, Class<?> clazz) {
		for (Class<?> type : acceptedTypes) {
			if (type.isArray())
				type = type.getComponentType();
			if (type.equals(Object.class) || type.isAssignableFrom(clazz)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Node getNode() {
		return node;
	}

	private String getCodeName(Class<?> clazz) {
		return Classes.getSuperClassInfo(clazz).getCodeName();
	}

	private String getOperatorString() {
		return switch (operator) {
			case MULTIPLICATION -> "multiplied";
			case DIVISION -> "divided";
			case EXPONENTIATION -> "exponentiated";
			default -> "";
		};
	}

	private void printError(boolean parseError, @Nullable Class<?> baseClass, @Nullable Class<?> operativeClass) {
		String message = "";
		if (baseClass == null) {
			if (operativeClass == null) {
				message = "This expression cannot be operated on.";
			} else {
				message = "This expression cannot be " + getOperatorString() + " by "
					+ Utils.a(getCodeName(operativeClass)) + ".";
			}
		} else {
			if (operativeClass == null) {
				message = Utils.A(getCodeName(baseClass)) + " cannot be " + getOperatorString() + ".";
			} else {
				message = Utils.A(getCodeName(baseClass)) + " cannot be " + getOperatorString() + " by "
					+ Utils.a(getCodeName(operativeClass)) + ".";
			}
		}
		if (parseError) {
			Skript.error(message);
		} else {
			error(message);
		}
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

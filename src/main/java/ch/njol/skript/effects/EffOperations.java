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
			Skript.error("This expression cannot be operated on.");
			return false;
		}
		operative = LiteralUtils.defendExpression(exprs[1]);
		node = getParser().getNode();
		Class<?> operativeType = operative.getReturnType();
		Class<?> baseType = base.getReturnType();
		// Ensure 'baseType' is referencing a non-array class
		if (baseType.isArray())
			baseType = baseType.getComponentType();
		if (operativeType.equals(Object.class) && baseType.equals(Object.class)) {
			// Both are object, so we should do the checks within '#execute'
			return LiteralUtils.canInitSafely(operative);
		} else if (operativeType.equals(Object.class) || baseType.equals(Object.class)) {
			// One is 'Object', so we can atleast see if the other has any operations registered
			Class<?>[] returnTypes = null;
			if (baseType.equals(Object.class)) {
				returnTypes = Arithmetics.getOperations(operator).stream()
					.filter(info -> info.getRight().isAssignableFrom(operativeType))
					.map(OperationInfo::getReturnType)
					.toArray(Class[]::new);
				if (returnTypes.length == 0) {
					Skript.error(operator.getName() + " cannot be performed with "
						+ Utils.a(getClassInfoCodeName(operativeType)));
					return false;
				}
			} else {
				returnTypes = Arithmetics.getOperations(operator, baseType).stream()
					.map(OperationInfo::getReturnType)
					.toArray(Class[]::new);
				if (returnTypes.length == 0) {
					Skript.error("This expression can not be " + getOperatorString() + ".");
					return false;
				}
			}
		} else {
			// We've deduced that the return types from both 'base' and 'operative' are guaranteed not to be 'Object.class'
			// We can check to see if an operation exists, if not, parse error
			operation = getOperation(baseType, operativeType);
			if (operation == null) {
				Skript.error("This expression cannot be " + getOperatorString() + " by "
					+ Utils.a(getClassInfoCodeName(operativeType)) + ".");
				return false;
			}
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
				operation = getOperation(baseType, operativeType);
				if (operation == null) {
					error(Utils.A(getClassInfoCodeName(baseType)) + " cannot be " + getOperatorString()
						+ " with " + Utils.a(getClassInfoCodeName(operativeType)) + ".");
					return;
				}
			}
			Operation<Object, Object, Object> finalOperation = operation;
			changerFunction = object -> finalOperation.calculate(object, operativeObject);
		} else {
			changerFunction = object -> {
				Operation<Object, Object, Object> thisOperation = getOperation(object.getClass(), operativeType);
				if (thisOperation != null)
					return thisOperation.calculate(object, operativeObject);
				return object;
			};
		}
		//noinspection unchecked,rawtypes
		base.changeInPlace(event, (Function) changerFunction);
	}

	private @Nullable Operation<Object, Object, Object> getOperation(Class<?> leftClass, Class<?> rightClass) {
		//noinspection unchecked
		return (Operation<Object, Object, Object>) Arithmetics.getOperation(operator, leftClass, rightClass, leftClass);
	}

	@Override
	public Node getNode() {
		return node;
	}

	private String getClassInfoCodeName(Class<?> clazz) {
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

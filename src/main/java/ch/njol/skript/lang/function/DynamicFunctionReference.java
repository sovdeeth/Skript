package ch.njol.skript.lang.function;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.lang.util.common.AnyNamed;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Contract;
import ch.njol.skript.util.Utils;
import ch.njol.skript.util.Utils.PluralResult;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.common.function.FunctionBinder;
import org.skriptlang.skript.common.function.FunctionBinder.Mode;
import org.skriptlang.skript.common.function.FunctionReference;
import org.skriptlang.skript.common.function.FunctionReference.Argument;
import org.skriptlang.skript.common.function.FunctionReference.ArgumentType;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.util.Executable;
import org.skriptlang.skript.util.Validated;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A partial reference to a Skript function.
 * This reference knows some of its information in advance (such as the function's name)
 * but will not be resolved until it receives inputs for the first time.
 * <p>
 * Which overload of a function this refers to is therefore only decided once arguments are
 * supplied, unless the parameter types were given when the reference was obtained.
 * </p>
 *
 */
public class DynamicFunctionReference
	implements Contract, Executable<Event, Object[]>, Validated, AnyNamed {

	/**
	 * Splits a stringified function reference into its name and, if one was written, the list of
	 * parameter types selecting an overload, e.g. the 'integer, objects' of
	 * 'myFunction(integer, objects)'.
	 */
	private static final Pattern SIGNATURE_PATTERN = Pattern.compile("(?<name>[^(]+)\\((?<types>.*)\\).*");

	private final @NotNull String name;

	/**
	 * The namespace local functions are resolved in, or null to only resolve global functions.
	 */
	private final @Nullable String namespace;

	/**
	 * The declared parameter types of the only overload this may use, or null if whichever
	 * overload the supplied arguments select may be used.
	 */
	private final Class<?> @Nullable [] parameterTypes;

	/**
	 * The namespace of the signature this resolved to, which is where the function was declared
	 * rather than where it was looked up from. Kept so that this keeps referring to the function
	 * it resolved to, rather than to whatever later takes the same name.
	 */
	private final @Nullable String declaredIn;

	private final Validated validator = Validated.validator();

	/**
	 * How a particular set of argument expressions binds to this function. Binding depends only
	 * on the expressions, not on the values they produce, so it is worked out once per input.
	 */
	private final Map<Input, Binding> bindings = new ConcurrentHashMap<>();

	/**
	 * A reusable {@link BoundExecutable} per set of argument expressions, so that binding does not allocate
	 * on every execution.
	 */
	private final Map<Input, BoundExecutable<Event, Object[]>> boundExecutables = new ConcurrentHashMap<>();

	/**
	 * The registry generation the cached bindings were resolved against. A script being reloaded
	 * replaces the {@link Function} objects of its functions, so a binding from before that must
	 * not be reused.
	 */
	private volatile long generation = -1;

	public DynamicFunctionReference(Function<?> function) {
		Signature<?> signature = function.getSignature();

		this.name = function.getName();
		this.namespace = signature.namespace();
		// a reference to a known function refers to exactly that overload
		this.parameterTypes = FunctionBinder.declaredTypes(signature);
		this.declaredIn = signature.namespace();
	}

	public DynamicFunctionReference(@NotNull String name) {
		this(name, null);
	}

	public DynamicFunctionReference(@NotNull String name, @Nullable Script source) {
		this(name, source, null);
	}

	/**
	 * @param name           The function name.
	 * @param source         The script the function is expected to be in, if one is known.
	 * @param parameterTypes The declared types of the parameters of the overload to use, or null
	 *                       to use whichever overload the supplied arguments select.
	 */
	public DynamicFunctionReference(@NotNull String name, @Nullable Script source, Class<?> @Nullable [] parameterTypes) {
		this.name = name;
		this.namespace = source != null ? source.getConfig().getFileName() : null;
		this.parameterTypes = parameterTypes;

		this.declaredIn = declaringNamespace(FunctionRegistry.getRegistry().getSignatures(namespace, name));
	}

	/**
	 * Whether this reference may use {@code signature}.
	 * <p>
	 * This is the only rule for which overloads a reference may resolve to: the function must be
	 * the one it originally resolved to, since another script may later declare a global function
	 * of the same name, and it must be the pinned overload if one was given.
	 * </p>
	 *
	 * @param signature The signature.
	 * @return Whether this may resolve to {@code signature}.
	 */
	private boolean accepts(Signature<?> signature) {
		if (!Objects.equals(signature.namespace(), declaredIn)) {
			return false;
		}
		return parameterTypes == null
			|| Arrays.equals(FunctionBinder.declaredTypes(signature), parameterTypes);
	}

	/**
	 * Picks the namespace to report as the source of this reference. A local lookup may fall back
	 * to a global function declared elsewhere, so this prefers a candidate declared in the
	 * namespace that was looked up.
	 *
	 * @param candidates The candidate signatures.
	 * @return The namespace the function was declared in, or null if that is not known.
	 */
	private @Nullable String declaringNamespace(Collection<Signature<?>> candidates) {
		String fallback = null;
		for (Signature<?> candidate : candidates) {
			if (Objects.equals(candidate.namespace(), namespace)) {
				return namespace;
			}
			if (fallback == null) {
				fallback = candidate.namespace();
			}
		}
		return fallback;
	}

	/**
	 * Parses the parameter type list of a stringified function reference, e.g. the
	 * 'integer, objects' of 'myFunction(integer, objects)', into the declared parameter types it
	 * names. This is the inverse of how a signature is written out in an error message.
	 *
	 * @param types The type list. Must not be blank.
	 * @return The declared types, or null if any of them could not be recognised.
	 */
	private static Class<?> @Nullable [] parseParameterTypes(@NotNull String types) {
		// a type name contains no commas, and the function ClassInfo is only parsed in contexts
		// where SkriptParser#next offers no nesting protection, so a plain split is all we can do
		String[] split = types.split(",", -1);
		Class<?>[] parsed = new Class<?>[split.length];

		for (int i = 0; i < split.length; i++) {
			String type = split[i].trim();
			if (type.isEmpty()) {
				reportBadType("Missing a parameter type in '" + types + "'");
				return null;
			}

			ClassInfo<?> classInfo = Classes.getClassInfoFromUserInput(type);
			PluralResult plural = Utils.isPlural(type);
			if (classInfo == null) {
				classInfo = Classes.getClassInfoFromUserInput(plural.updated());
			}

			if (classInfo == null) {
				reportBadType("Cannot recognise the type '" + type + "'");
				return null;
			}

			parsed[i] = plural.plural() ? classInfo.getC().arrayType() : classInfo.getC();
		}

		return parsed;
	}

	/**
	 * Reports a parameter type list which could not be understood.
	 * <p>
	 * A reference can be resolved either while a script is being parsed, where this belongs in the
	 * parse log, or at runtime, where there is no telling which log is listening. Only the former
	 * is reported; resolving a name that does not exist has always been silent at runtime.
	 * </p>
	 *
	 * @param message The message.
	 */
	private static void reportBadType(String message) {
		if (ParserInstance.get().isActive()) {
			Skript.error(message);
		}
	}

	public @Nullable Script source() {
		return ScriptLoader.getLoadedScriptFromName(declaredIn);
	}

	@Override
	public @NotNull String name() {
		return name;
	}

	@Override
	public boolean isSingle(Expression<?>... arguments) {
		FunctionReference<?> reference = binding(new Input(arguments)).reference();
		if (reference == null) {
			return true;
		}
		return reference.isSingle();
	}

	@Override
	public @Nullable Class<?> getReturnType(Expression<?>... arguments) {
		FunctionReference<?> reference = binding(new Input(arguments)).reference();
		if (reference == null) {
			return Object.class;
		}

		Contract contract = reference.signature().contract();
		if (contract != null) {
			return contract.getReturnType(arguments);
		}
		return reference.signature().returnType();
	}

	/**
	 * Executes this function with the given arguments.
	 *
	 * @param event The event to execute with.
	 * @param input The argument expressions, as collected when the caller was parsed.
	 * @return The returned values, or null if the arguments are not acceptable or execution failed.
	 */
	public Object @Nullable [] execute(Event event, Input input) {
		return execute(event, input, binding(input));
	}

	private Object @Nullable [] execute(Event event, Input input, Binding binding) {
		// deliberately not valid(): a binding is already discarded when the registered functions
		// change, so scanning the registry again here would only turn a function which no longer
		// exists into a silent failure, where binding it reports why
		if (!validator.valid()) {
			return null;
		}

		FunctionReference<?> reference = binding.reference();

		if (reference == null) {
			if (binding.spread() < 0) { // the reason is available from BoundExecutable#rejection
				return null;
			}
			// the arguments only fit once the values of one of them are spread across several
			// parameters, which cannot be known before they have been evaluated
			reference = spread(event, input, binding.spread());
			if (reference == null) {
				return null;
			}
		}

		try {
			return normalise(reference, reference.execute(event));
		} finally {
			reset(reference);
		}
	}

	/**
	 * Resets the return value of the function {@code reference} refers to.
	 * {@link FunctionReference#execute(Event)} does not do this itself, and a script function
	 * asserts that its return value was reset before it is set again.
	 *
	 * @param reference The reference which was executed.
	 */
	private static void reset(FunctionReference<?> reference) {
		org.skriptlang.skript.common.function.Function<?> function = reference.function();
		if (function != null) {
			function.resetReturnValue();
		}
	}

	/**
	 * @param reference The reference which was executed.
	 * @param result    The value the function returned.
	 * @return That value as an array, since a function may return either a single value or
	 * 	several, or an empty array if it returned nothing. Never null, so that a null result from
	 * 	{@link #execute(Event, Input)} only ever means the function did not run.
	 */
	private Object[] normalise(FunctionReference<?> reference, @Nullable Object result) {
		// a function returning several values already hands back an array of its own return type
		if (result instanceof Object[] array) {
			return array;
		}

		// the array is built with the function's return type as its component type, matching what
		// a function returning several values gives back, rather than always being an Object[]
		Class<?> returnType = reference.signature().returnType();
		Class<?> component = returnType != null ? Utils.getComponentType(returnType) : Object.class;

		Object[] array = (Object[]) Array.newInstance(component, result != null ? 1 : 0);
		if (result != null) {
			array[0] = result;
		}
		return array;
	}

	/**
	 * Binds the arguments of {@code input} by spreading the values of the argument at
	 * {@code index} across several parameters.
	 * <p>
	 * This exists so that passing a single list of values to a function of several parameters
	 * keeps working. How many parameters those values fill is only known once they have been
	 * evaluated, so unlike every other binding this one is worked out for each execution.
	 * </p>
	 *
	 * @param event The event to evaluate with.
	 * @param input The argument expressions.
	 * @param index The index of the argument whose values are spread.
	 * @return The bound reference, or null if the values do not fit any overload either.
	 */
	private @Nullable FunctionReference<?> spread(Event event, Input input, int index) {
		Expression<?>[] expressions = input.expressions();

		List<Argument<Expression<?>>> arguments = new ArrayList<>(expressions.length);
		for (int i = 0; i < expressions.length; i++) {
			if (i != index) {
				arguments.add(argument(expressions[i]));
				continue;
			}

			for (Object value : expressions[i].getArray(event)) {
				arguments.add(argument(new SimpleLiteral<>(value, true)));
			}
		}

		//noinspection unchecked
		Argument<Expression<?>>[] array = arguments.toArray(new Argument[0]);

		try (RetainingLogHandler log = SkriptLogger.startRetainingLog()) {
			FunctionReference<?> reference =
				FunctionBinder.forExpressions(Mode.GREEDY).resolve(namespace, name, array, this::accepts);
			discard(log);
			return reference;
		}
	}

	/**
	 * @param input The argument expressions.
	 * @return How those arguments bind to this function, working it out if it is not yet known.
	 */
	private Binding binding(Input input) {
		// a reload replaces the functions this may have bound to, and may equally have added the
		// overload a previously failed binding was looking for, so both outcomes are discarded
		long current = FunctionRegistry.getRegistry().generation();
		if (generation != current) {
			bindings.clear();
			generation = current;
		}

		Binding binding = bindings.get(input);
		if (binding != null) {
			return binding;
		}

		binding = computeBinding(input);
		bindings.put(input, binding);
		return binding;
	}

	/**
	 * Works out how the arguments of {@code input} bind to this function.
	 *
	 * @param input The argument expressions.
	 * @return The binding, which may be one that does not fit.
	 */
	private Binding computeBinding(Input input) {
		Expression<?>[] expressions = input.expressions();

		//noinspection unchecked
		Argument<Expression<?>>[] arguments = (Argument<Expression<?>>[]) new Argument[expressions.length];
		for (int i = 0; i < expressions.length; i++) {
			arguments[i] = argument(expressions[i]);
		}

		// an argument which is not single may have to be spread across several parameters, which
		// can only be decided once its values are known, so a failure here is not yet final
		int spread = spreadable(expressions);

		try (RetainingLogHandler log = SkriptLogger.startRetainingLog()) {
			FunctionReference<?> reference =
				FunctionBinder.forExpressions(Mode.GREEDY).resolve(namespace, name, arguments, this::accepts);

			if (reference != null) {
				discard(log);
				return new Binding(reference, -1);
			}

			if (spread >= 0) { // the spread may still bind, so do not report this failure
				discard(log);
				return new Binding(null, spread);
			}

			LogEntry first = null;
			for (LogEntry entry : log.getLog()) { // marks the log as handled
				if (entry.getLevel().intValue() >= Level.SEVERE.intValue()) {
					first = entry;
					break;
				}
			}

			return new Binding(null, -1, first != null ? first.getMessage() : null);
		}
	}

	/**
	 * Throws away everything a speculative binding logged.
	 *
	 * @param log The log to discard.
	 */
	private static void discard(RetainingLogHandler log) {
		log.clear();
		// clearing alone does not count as having handled the log, and the handler complains when
		// it is stopped without having been told what to do with it
		log.printLog();
	}

	/**
	 * @param expressions The argument expressions.
	 * @return The index of the only argument whose values could be spread across several
	 * 	parameters, or -1 if there is not exactly one such argument.
	 */
	private static int spreadable(Expression<?>[] expressions) {
		int spread = -1;
		for (int i = 0; i < expressions.length; i++) {
			if (expressions[i].isSingle()) {
				continue;
			}
			if (spread >= 0) { // with more than one, which to spread would be arbitrary
				return -1;
			}
			spread = i;
		}
		return spread;
	}

	private static Argument<Expression<?>> argument(Expression<?> expression) {
		return new Argument<>(ArgumentType.UNNAMED, null, expression);
	}

	@Override
	public @Nullable Executable.BoundExecutable<Event, Object[]> bind(Expression<?>... arguments) {
		// a function always decides for itself which parameter each argument belongs to, and
		// whether they are acceptable may depend on their values, so that is left to execution
		return boundExecutables.computeIfAbsent(new Input(arguments), Bound::new);
	}

	@Override
	public Object @Nullable [] execute(Event event, Object... arguments) {
		//noinspection unchecked
		Argument<Expression<?>>[] bound = (Argument<Expression<?>>[]) new Argument[arguments.length];
		for (int i = 0; i < arguments.length; i++) {
			bound[i] = argument(literal(arguments[i]));
		}

		// the values are new on every call, so there is nothing worth caching here
		FunctionReference<?> reference;
		try (RetainingLogHandler log = SkriptLogger.startRetainingLog()) {
			reference = FunctionBinder.forExpressions(Mode.GREEDY)
				.resolve(namespace, name, bound, this::accepts);
			discard(log);
		}

		if (reference == null) {
			return null;
		}

		try {
			return normalise(reference, reference.execute(event));
		} finally {
			reset(reference);
		}
	}

	/**
	 * Represents an argument value passed to the positional form of
	 * {@link #execute(Event, Object...)} as an expression.
	 *
	 * @param value The value, which may be an array of values for a list parameter, or null to
	 *              leave the parameter to its default.
	 * @return The expression, or null if the parameter should use its default.
	 */
	private static @Nullable Expression<?> literal(@Nullable Object value) {
		if (value == null) { // the parameter was omitted
			return null;
		}

		if (value instanceof Object[] values) { // several values, for a list parameter
			return new SimpleLiteral<>(values, Object.class, true);
		}

		return new SimpleLiteral<>(value, true);
	}

	@Override
	public void invalidate() {
		this.validator.invalidate();
	}

	@Override
	public boolean valid() {
		if (!validator.valid()) {
			return false;
		}

		for (Signature<?> signature : FunctionRegistry.getRegistry().getSignatures(namespace, name)) {
			if (accepts(signature)) {
				return true;
			}
		}
		return false;
	}


	@Override
	public String toString() {
		Script source = source();
		if (source != null)
			return name + "() from " + Classes.toString(source);
		return name + "()";
	}

	/**
	 * Validates whether dynamic inputs are appropriate for the resolved function.
	 *
	 * @param parameters The input types to check
	 * @return A combined expression list, if these inputs are appropriate for the function
	 * @deprecated Use {@link #execute(Event, Input)}, which binds each argument to the parameter
	 * 	it belongs to instead of collapsing them all into one expression.
	 */
	@Deprecated(forRemoval = true, since = "2.18")
	public @Nullable Expression<?> validate(Expression<?>[] parameters) {
		return this.validate(new Input(parameters));
	}

	/**
	 * @deprecated Use {@link #execute(Event, Input)}, which binds each argument to the parameter
	 * 	it belongs to instead of collapsing them all into one expression.
	 */
	@Deprecated(forRemoval = true, since = "2.18")
	public @Nullable Expression<?> validate(Input input) {
		if (!binding(input).bindable()) {
			return null;
		}

		// the arguments are acceptable, but which parameter each of them belongs to is only known
		// to the binding, so handing them back as one expression loses that
		return new ExpressionList<>(input.expressions(), Object.class, true);
	}

	/**
	 * Attempts to parse a function reference from the format it would be stringified in.
	 * The name can include the source script name for the case of parsing local functions.
	 * @param name The function name, possibly including its script name
	 * @return A reference, if one is available
	 */
	public static @Nullable DynamicFunctionReference parseFunction(String name) {
		// Function reference string-ifying appends a () and potentially its source,
		// e.g. `myFunction() from MyScript.sk` and we should turn that into a valid function.
		if (name.contains(") from ")) {
			// The user might be trying to resolve a local function by name only
			String source = name.substring(name.lastIndexOf(" from ") + 6).trim();
			Script script = ScriptLoader.getLoadedScriptFromName(source);
			return resolveFunction(name.substring(0, name.lastIndexOf(" from ")).trim(), script);
		}
		return resolveFunction(name, null);
	}

	/**
	 * Used to resolve a function from its name.
	 * @param name The function name
	 * @param script Potentially, the script it is from, if one is known
	 * @return A function reference, if one is available.
	 */
	public static @Nullable DynamicFunctionReference resolveFunction(String name, @Nullable Script script) {
		Class<?>[] parameterTypes = null;

		Matcher matcher = SIGNATURE_PATTERN.matcher(name);
		if (matcher.matches()) {
			String types = matcher.group("types");
			name = matcher.group("name").trim();

			// 'myFunction()' has always meant the function of that name, whatever its parameters,
			// so only a non-empty list selects a particular overload
			if (!types.isBlank()) {
				parameterTypes = parseParameterTypes(types);
				if (parameterTypes == null) {
					return null;
				}
			}
		}

		DynamicFunctionReference reference = new DynamicFunctionReference(name, script, parameterTypes);
		if (!reference.valid())
			return null;
		return reference;
	}

	/**
	 * This function bound to a particular set of argument expressions.
	 */
	private final class Bound implements BoundExecutable<Event, Object[]> {

		private final Input input;

		private Bound(Input input) {
			this.input = input;
		}

		@Override
		public Object @Nullable [] execute(Event event) {
			return DynamicFunctionReference.this.execute(event, input);
		}

		@Override
		public @Nullable String rejection() {
			Binding binding = binding(input);
			// a binding which needs a spread cannot be judged until its values are known
			return binding.bindable() ? null : binding.error();
		}

	}

	/**
	 * How a set of argument expressions binds to a function.
	 *
	 * @param reference The reference the arguments were bound to, or null if they could not be
	 *                  bound without knowing their values.
	 * @param spread    The index of the argument whose values must be spread across several
	 *                  parameters for the arguments to fit, or -1 if there is none.
	 * @param error     Why the arguments could not be bound, if they could not be.
	 */
	private record Binding(@Nullable FunctionReference<?> reference, int spread, @Nullable String error) {

		private Binding(@Nullable FunctionReference<?> reference, int spread) {
			this(reference, spread, null);
		}

		/**
		 * @return Whether the arguments fit, possibly only once their values are known.
		 */
		private boolean bindable() {
			return reference != null || spread >= 0;
		}

	}

	/**
	 * An index-linking key for a particular set of argument expressions.
	 * Binding only needs to be done once for a set of argument types, so this is used to avoid
	 * re-binding on every execution.
	 */
	public static class Input {

		private final Class<?>[] types;
		private transient final Expression<?>[] expressions;

		public Input(Expression<?>... expressions) {
			Class<?>[] types = new Class<?>[expressions.length];
			for (int i = 0; i < expressions.length; i++) {
				types[i] = expressions[i].getReturnType();
			}
			this.expressions = expressions;
			this.types = types;
		}

		private Expression<?>[] expressions() {
			return expressions;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object)
				return true;
			if (!(object instanceof Input input))
				return false;
			return Arrays.equals(expressions, input.expressions) && Objects.deepEquals(types, input.types);
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(types) ^ Arrays.hashCode(expressions);
		}

	}

}

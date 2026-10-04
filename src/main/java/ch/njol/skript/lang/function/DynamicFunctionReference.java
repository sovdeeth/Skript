package ch.njol.skript.lang.function;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.util.common.AnyNamed;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Contract;
import ch.njol.skript.util.Utils;
import ch.njol.skript.util.Utils.PluralResult;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.skriptlang.skript.common.function.Parameter;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.util.Executable;
import org.skriptlang.skript.util.Validated;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A partial reference to a Skript function.
 * This reference knows some of its information in advance (such as the function's name)
 * but will not be resolved until it receives inputs for the first time.
 * @param <Result> The return type of this function, if known.
 */
public class DynamicFunctionReference<Result>
	implements Contract, Executable<Event, Result[]>, Validated, AnyNamed {

	/**
	 * Splits a stringified function reference into its name and, if one was written, the list of
	 * parameter types selecting an overload, e.g. the 'integer, objects' of
	 * 'myFunction(integer, objects)'.
	 */
	private static final Pattern SIGNATURE_PATTERN = Pattern.compile("(?<name>[^(]+)\\((?<types>.*)\\).*");

	private final @NotNull String name;
	private final @Nullable Script source;
	private final Reference<Function<? extends Result>> function;
	private final @UnknownNullability Signature<? extends Result> signature;
	private final Validated validator = Validated.validator();
	private final Map<Input, Expression<?>> checkedInputs = new HashMap<>();
	private final boolean resolved;

	public DynamicFunctionReference(Function<? extends Result> function) {
		this.resolved = true;
		this.function = new WeakReference<>(function);
		this.name = function.getName();
		this.signature = function.getSignature();
		this.source = ScriptLoader.getLoadedScriptFromName(signature.namespace());
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
	 *                       to use whichever overload is found for the name alone.
	 */
	public DynamicFunctionReference(@NotNull String name, @Nullable Script source, Class<?> @Nullable [] parameterTypes) {
		this.name = name;
		String namespace = source != null ? source.getConfig().getFileName() : null;

		//noinspection unchecked
		Function<? extends Result> function =
			(Function<? extends Result>) findFunction(name, namespace, parameterTypes);

		this.resolved = function != null;
		this.function = new WeakReference<>(function);
		if (resolved) {
			this.signature = function.getSignature();
			// A local lookup may still fall back to a global function from another script,
			// so only reuse the provided script when the function actually came from it.
			if (source != null && source.getConfig().getFileName().equals(signature.namespace())) {
				this.source = source;
			} else {
				this.source = ScriptLoader.getLoadedScriptFromName(signature.namespace());
			}
		} else {
			this.signature = null;
			this.source = null;
		}
	}

	/**
	 * Finds the function to use for a name, optionally restricted to the overload declaring
	 * exactly {@code parameterTypes}.
	 *
	 * @param name           The function name.
	 * @param namespace      The namespace to look in, or null for global functions only.
	 * @param parameterTypes The declared parameter types of the overload to use, or null for any.
	 * @return The function, or null if there is none.
	 */
	private static @Nullable Function<?> findFunction(
		@NotNull String name, @Nullable String namespace, Class<?> @Nullable [] parameterTypes
	) {
		if (parameterTypes == null) {
			return Functions.getFunction(name, namespace);
		}

		FunctionRegistry registry = FunctionRegistry.getRegistry();
		for (Signature<?> candidate : registry.getSignatures(namespace, name)) {
			if (Arrays.equals(declaredTypes(candidate), parameterTypes)) {
				return registry.getFunction(candidate);
			}
		}

		return null;
	}

	/**
	 * @param signature The signature.
	 * @return The declared types of the parameters of {@code signature}, in order.
	 */
	private static Class<?>[] declaredTypes(Signature<?> signature) {
		return Arrays.stream(signature.parameters().all())
			.map(Parameter::type)
			.toArray(Class<?>[]::new);
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
				Skript.error("Missing a parameter type in '" + types + "'");
				return null;
			}

			ClassInfo<?> classInfo = Classes.getClassInfoFromUserInput(type);
			PluralResult plural = Utils.isPlural(type);
			if (classInfo == null) {
				classInfo = Classes.getClassInfoFromUserInput(plural.updated());
			}

			if (classInfo == null) {
				Skript.error("Cannot recognise the type '" + type + "'");
				return null;
			}

			parsed[i] = plural.plural() ? classInfo.getC().arrayType() : classInfo.getC();
		}

		return parsed;
	}

	public @Nullable Script source() {
		return source;
	}

	@Override
	public @NotNull String name() {
		return name;
	}

	@Override
	public boolean isSingle(Expression<?>... arguments) {
		if (!resolved)
			return true;
		return signature.getContract() != null
				? signature.getContract().isSingle(arguments)
				: signature.isSingle();
	}

	@Override
	public @Nullable Class<?> getReturnType(Expression<?>... arguments) {
		if (!resolved)
			return Object.class;
		if (signature.getContract() != null)
			return signature.getContract().getReturnType(arguments);
		Function<? extends Result> function = this.function.get();
		if (function != null && function.getReturnType() != null)
			return function.getReturnType().getC();
		return null;
	}

	@Override
	public Result @Nullable [] execute(Event event, Object... arguments) {
		if (!this.valid())
			return null;
		Function<? extends Result> function = this.function.get();
		if (function == null)
			return null;
		// We shouldn't trust the caller provided an array of arrays
		Object[][] consigned = FunctionReference.consign(arguments);
		try {
			return function.execute(consigned);
		} finally {
			function.resetReturnValue();
		}
	}

	@Override
	public void invalidate() {
		this.validator.invalidate();
	}

	@Override
	public boolean valid() {
		return resolved && validator.valid()
			&& function.get() != null // function was garbage-collected
			// Deliberately checks the config rather than calling Script#valid(),
			// which additionally stats the script file on every call.
			// We should revisit script validity in general since it's technically fine
			// for the script to not have a File, but for this case all we care about is whether
			// the config is still loaded and valid.
			&& (source == null || source.getConfig().valid());
	}

	@Override
	public String toString() {
		if (source != null)
			return name + "() from " + Classes.toString(source);
		return name + "()";
	}

	/**
	 * Validates whether dynamic inputs are appropriate for the resolved function.
	 * If the inputs are acceptable, this will collect them into an expression list
	 * (the output of which can be passed directly to the task).
	 *
	 * @param parameters The input types to check
	 * @return A combined expression list, if these inputs are appropriate for the function
	 */
	public @Nullable Expression<?> validate(Expression<?>[] parameters) {
		Input input = new Input(parameters);
		return this.validate(input);
	}

	public @Nullable Expression<?> validate(Input input) {
		if (checkedInputs.containsKey(input))
			return checkedInputs.get(input);
		this.checkedInputs.put(input, null); // failure case
		if (signature == null)
			return null;
		boolean varArgs = signature.getMaxParameters() == 1 && !signature.parameters().getFirst().isSingle();
		Expression<?>[] inputParameters = input.parameters();
		// Too many parameters
		if (inputParameters.length > signature.getMaxParameters() && !varArgs)
			return null;
		// Not enough parameters
		else if (inputParameters.length < signature.getMinParameters())
			return null;
		Expression<?>[] checkedInputParameters = new Expression[inputParameters.length];

		// Check parameter types
		for (int i = 0; i < inputParameters.length; i++) {
			Parameter<?> parameter = signature.parameters().all()[varArgs ? 0 : i];

			Class<?> target = Utils.getComponentType(parameter.type());
			//noinspection unchecked
			Expression<?> expression = inputParameters[i].getConvertedExpression(target);
			if (expression == null) {
				return null;
			} else if (parameter.isSingle() && !expression.isSingle()) {
				return null;
			}
			checkedInputParameters[i] = expression;
		}

		// if successful, replace with our known result
		ExpressionList<?> result = new ExpressionList<>(checkedInputParameters, Object.class, true);
		this.checkedInputs.put(input, result);
		return result;
	}

	/**
	 * Attempts to parse a function reference from the format it would be stringified in.
	 * The name can include the source script name for the case of parsing local functions.
	 * @param name The function name, possibly including its script name
	 * @return A reference, if one is available
	 */
	public static @Nullable DynamicFunctionReference<?> parseFunction(String name) {
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
	public static @Nullable DynamicFunctionReference<?> resolveFunction(String name, @Nullable Script script) {
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

		DynamicFunctionReference<Object> reference = new DynamicFunctionReference<>(name, script, parameterTypes);
		if (!reference.valid())
			return null;
		return reference;
	}

	/**
	 * An index-linking key for a particular set of input expressions.
	 * Validation only needs to be done once for a set of parameter types,
	 * so this is used to prevent re-validation.
	 */
	public static class Input {
		private final Class<?>[] types;
		private transient final Expression<?>[] parameters;

		public Input(Expression<?>... types) {
			Class<?>[] classes = new Class<?>[types.length];
			for (int i = 0; i < types.length; i++) {
				classes[i] = types[i].getReturnType();
			}
			this.parameters = types;
			this.types = classes;
		}

		private Expression<?>[] parameters() {
			return parameters;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object)
				return true;
			if (!(object instanceof Input input))
				return false;
			return Arrays.equals(parameters, input.parameters) && Objects.deepEquals(types, input.types);
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(types) ^ Arrays.hashCode(parameters);
		}

	}

}

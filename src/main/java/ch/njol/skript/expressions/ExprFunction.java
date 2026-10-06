package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.function.DynamicFunctionReference;
import ch.njol.skript.lang.function.FunctionRegistry;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import ch.njol.skript.registrations.experiments.ReflectionExperimentSyntax;
import org.skriptlang.skript.lang.script.Script;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Name("Function")
@Description("Obtain a function by name, which can be executed.")
@Example("set {_function} to the function named \"myFunction\"")
@Example("run {_function} with arguments 13 and true")
@Since("2.10")
public class ExprFunction extends SimpleExpression<DynamicFunctionReference> implements ReflectionExperimentSyntax {

	static {
		Skript.registerExpression(ExprFunction.class, DynamicFunctionReference.class, ExpressionType.COMBINED,
				"[the|a] function [named] %string% [(in|from) %-script%]",
				"[the] functions [named] %strings% [(in|from) %-script%]",
				"[all [[of] the]|the] functions (in|from) %script%"
		);
	}

	/**
	 * How many names one of these keeps resolved references for. A name built at runtime could
	 * otherwise grow the cache without bound, so it is emptied once it gets this large.
	 */
	private static final int MAX_CACHED_NAMES = 64;

	private Expression<String> name;
	private Expression<Script> script;
	private int mode;
	private boolean local;
	private Script here;

	/**
	 * References already resolved by this expression, since it is evaluated once per call rather
	 * than once per parse, and resolving allocates a reference and scans the registry for it.
	 */
	private final Map<Key, DynamicFunctionReference> resolved = new ConcurrentHashMap<>();

	/**
	 * The registry generation {@link #resolved} was filled against. Registering or removing a
	 * function moves it, which is what makes a kept reference safe to reuse until then.
	 */
	private volatile long generation = -1;

	@Override
	@SuppressWarnings("null")
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult result) {
		this.mode = matchedPattern;
		this.local = mode == 2 || expressions[1] != null;
		switch (mode) {
			case 0, 1 -> {
				//noinspection unchecked
				this.name = (Expression<String>) expressions[0];
				if (local)
					//noinspection unchecked
					this.script = (Expression<Script>) expressions[1];
			}
			case 2 ->
				//noinspection unchecked
				this.script = (Expression<Script>) expressions[0];
		}
		this.here = this.getParser().getCurrentScript();
		return true;
	}

	/**
	 * @param name   The function name to resolve.
	 * @param script The script to resolve a local function in, or null for a global one.
	 * @return The reference, or null if no function of that name is available.
	 */
	private @Nullable DynamicFunctionReference resolve(String name, @Nullable Script script) {
		long current = FunctionRegistry.getRegistry().generation();
		if (generation != current || resolved.size() > MAX_CACHED_NAMES) {
			resolved.clear();
			generation = current;
		}

		// a name which resolves to nothing is not kept: it cannot be stored beside the others and
		// is not worth a sentinel, since a call to a function which does not exist is not a case
		// worth making fast
		return resolved.computeIfAbsent(new Key(name, script),
			key -> DynamicFunctionReference.resolveFunction(key.name(), key.script()));
	}

	/**
	 * A name resolved against a particular script.
	 *
	 * @param name   The function name.
	 * @param script The script a local function was looked for in, or null for a global one.
	 */
	private record Key(String name, @Nullable Script script) {

	}

	@Override
	protected DynamicFunctionReference[] get(Event event) {
		@Nullable Script script;
		if (local) {
			script = this.script.getSingle(event);
		} else {
			script = here;
		}
		return switch (mode) {
			case 0 -> {
				@Nullable String name = this.name.getSingle(event);
				if (name == null)
					yield CollectionUtils.array();
				@Nullable DynamicFunctionReference reference = resolve(name, script);
				if (reference == null)
					yield CollectionUtils.array();
				yield CollectionUtils.array(reference);
			}
			case 1 -> this.name.stream(event).map(string -> resolve(string, script))
					.filter(Objects::nonNull)
					.toArray(DynamicFunctionReference[]::new);
			case 2 -> {
				if (script == null)
					yield CollectionUtils.array();
				yield FunctionRegistry.getRegistry()
					.getDeclaredFunctions(script.getConfig().getFileName()).stream()
					.map(DynamicFunctionReference::new)
					.toArray(DynamicFunctionReference[]::new);
			}
			default -> throw new IllegalStateException("Unexpected value: " + mode);
		};
	}

	@Override
	public boolean isSingle() {
		return mode != 2 && name.isSingle();
	}

	@Override
	public Class<? extends DynamicFunctionReference> getReturnType() {
		return DynamicFunctionReference.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return switch (mode) {
			case 0 -> "the function named " + name.toString(event, debug)
						+ (local ? " from " + script.toString(event, debug) : "");
			case 1 -> "functions named " + name.toString(event, debug)
						+ (local ? " from " + script.toString(event, debug) : "");
			case 2 -> "the functions from " + script.toString(event, debug);
			default -> throw new IllegalStateException("Unexpected value: " + mode);
		};
	}

}

package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.config.Node;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.Kleenean;
import org.skriptlang.skript.log.runtime.SyntaxRuntimeErrorProducer;

import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import ch.njol.skript.registrations.experiments.ReflectionExperimentSyntax;
import org.skriptlang.skript.util.Executable;

@Name("Run")
@Description("Executes a task (a function). Any returned result is discarded.")
@Example("""
	set {_function} to the function named "myFunction"
	run {_function}
	run {_function} with arguments {_things::*}
	""")
@Since("2.10")
@Keywords({"run", "execute", "reflection", "function"})
@SuppressWarnings({"rawtypes", "unchecked"})
public class EffRun extends Effect implements ReflectionExperimentSyntax, SyntaxRuntimeErrorProducer {

	static {
		Skript.registerEffect(EffRun.class,
				"run %executable% [arguments:with arg[ument]s %-objects%]",
				"execute %executable% [arguments:with arg[ument]s %-objects%]");
	}

	// We don't bother with the generic type here because we have no way to verify it
	// from the expression, and it makes casting more difficult to no benefit.
	private Expression<Executable> executable;
	private Expression<?> arguments;
	private Expression<?>[] argumentExpressions;
	private Node node;
	private boolean hasArguments;

	@Override
	public boolean init(Expression<?>[] expressions, int pattern, Kleenean isDelayed, ParseResult result) {
		this.node = getParser().getNode();
		this.executable = ((Expression<Executable>) expressions[0]);
		this.hasArguments = result.hasTag("arguments");
		if (hasArguments) {
			this.arguments = LiteralUtils.defendExpression(expressions[1]);
			Expression<?>[] arguments;
			if (this.arguments instanceof ExpressionList<?>) {
				arguments = ((ExpressionList<?>) this.arguments).getExpressions();
			} else {
				arguments = new Expression[]{this.arguments};
			}
			this.argumentExpressions = arguments;
			return LiteralUtils.canInitSafely(this.arguments);
		} else {
			this.argumentExpressions = new Expression[0];
		}
		return true;
	}

	@Override
	protected void execute(Event event) {
		Executable task = executable.getSingle(event);
		if (task == null)
			return;
		// something which decides for itself what its arguments mean, such as a function with
		// parameters, is handed the argument expressions rather than a flat list of their values
		Executable.BoundExecutable<Event, ?> boundExecutable = task.bind(argumentExpressions);
		if (boundExecutable != null) {
			run(task, boundExecutable, event);
			return;
		}

		Object[] arguments;
		if (hasArguments) {
			arguments = this.arguments.getArray(event);
		} else {
			arguments = new Object[0];
		}
		task.execute(event, arguments);
	}

	/**
	 * Executes {@code boundExecutable}, reporting as a runtime error why it would not run, since
	 * it knows why its arguments were not acceptable but not where the call to it was written.
	 *
	 * @param executable      The executable that was bound.
	 * @param boundExecutable The bound executable.
	 * @param event           The event to execute with.
	 * @return The result, or null if it did not execute.
	 */
	private <T> @Nullable T run(
		Executable<Event, ?> executable, Executable.BoundExecutable<Event, T> boundExecutable, Event event
	) {
		T result = boundExecutable.execute(event);
		if (result != null) {
			return result;
		}

		// the executable knows why it would not accept its arguments, but only the call site knows
		// where the call was written
		String rejection = boundExecutable.rejection();
		error(rejection != null
			? rejection
			: "Cannot run " + Classes.toString(executable) + " with the given arguments.");

		return null;
	}

	@Override
	public Node getNode() {
		return node;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		if (hasArguments)
			return "run " + executable.toString(event, debug) + " with arguments " + arguments.toString(event, debug);
		return "run " + executable.toString(event, debug);
	}

}

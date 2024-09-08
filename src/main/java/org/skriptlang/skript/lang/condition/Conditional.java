package org.skriptlang.skript.lang.condition;

import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Contract;

/**
 * An object which can evaluate to `true`, `false`, or `unknown`.
 * `unknown` is currently unused, but intended for future handling of unexpected runtime situations, where some aspect of
 * the condition in ill-defined by the user and would result in ambiguous or undefined behavior.
 */
// TODO: replace Bukkit event with proper context object
public interface Conditional {

	/**
	 * Evaluates this object as `true`, `false`, or `unknown`.
	 * This value may change between subsequent callings.
	 *
	 * @param event The event with which to evaluate this object.
	 * @return The evaluation of this object.
	 */
	@Contract(pure = true)
	Kleenean evaluate(Event event);

	/**
	 * Computes {@link Kleenean#and(Kleenean)} with the evaluations of this {@link Conditional} and the other.
	 * Evaluates the other first, and shortcuts if it is not {@link Kleenean#TRUE}.
	 *
	 * @param other The {@link Conditional} to AND with. Will always be evaluated.
	 * @param event The event with which to evaluate the conditionals.
	 * @return The result of {@link Kleenean#and(Kleenean)}, given the evaluations of the two conditionals.
	 */
	@Contract(pure = true)
	default Kleenean and(Conditional other, Event event) {
		return and(other.evaluate(event), event);
	}

	/**
	 * Computes {@link Kleenean#and(Kleenean)} with the evaluation of this {@link Conditional} and the given Kleenean.
	 * Evaluates this object iff the given {@link Kleenean} is {@link Kleenean#TRUE}.
	 *
	 * @param other The {@link Kleenean} to AND with.
	 * @param event The event with which to evaluate the conditional, if necessary.
	 * @return The result of {@link Kleenean#and(Kleenean)}, given the evaluation of the conditional.
	 */
	@Contract(pure = true)
	default Kleenean and(Kleenean other, Event event) {
		if (!other.isTrue())
			return other;
		return evaluate(event);
	}

	/**
	 * Computes {@link Kleenean#or(Kleenean)} with the evaluations of this {@link Conditional} and the other.
	 * Evaluates the other first, and shortcuts if it is not {@link Kleenean#TRUE}.
	 *
	 * @param other The {@link Conditional} to OR with. Will always be evaluated.
	 * @param event The event with which to evaluate the conditionals.
	 * @return The result of {@link Kleenean#or(Kleenean)}, given the evaluations of the two conditionals.
	 */
	@Contract(pure = true)
	default Kleenean or(Conditional other, Event event) {
		return or(other.evaluate(event), event);
	}

	/**
	 * Computes {@link Kleenean#or(Kleenean)} with the evaluation of this {@link Conditional} and the given Kleenean.
	 * Evaluates this object iff the given {@link Kleenean} is {@link Kleenean#FALSE} or {@link Kleenean#UNKNOWN}.
	 *
	 * @param other The {@link Kleenean} to OR with.
	 * @param event The event with which to evaluate the conditional, if necessary.
	 * @return The result of {@link Kleenean#or(Kleenean)}, given the evaluation of the conditional.
	 */
	@Contract(pure = true)
	default Kleenean or(Kleenean other, Event event) {
		if (other.isTrue())
			return other;
		return evaluate(event);
	}

	/**
	 * Computes {@link Kleenean#not()} on the evaluation of this {@link Conditional}.
	 * @param event The event with which to evaluate the conditional.
	 * @return The result of {@link Kleenean#not()}, given the evaluation of the conditional.
	 */
	@Contract(pure = true)
	default Kleenean not(Event event) {
		return this.evaluate(event).not();
	}

	static ConditionalBuilder builder() {
		return new ConditionalBuilder();
	}

	enum Operator {
		AND,
		OR,
		NOT
	}
}

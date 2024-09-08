package org.skriptlang.skript.lang.condition;

import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

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
	 * Evaluates this object as `true`, `false`, or `unknown`.
	 * This value may change between subsequent callings.
	 * Uses a mutable cache of evaluated conditionals to prevent duplicate evaluations.
	 *
	 * @param event The event with which to evaluate this object.
	 * @param cache The cache of evaluated conditionals.
	 * @return The evaluation of this object.
	 */
	@Contract(pure = true)
	default Kleenean evaluate(Event event, @Nullable Map<Conditional, Kleenean> cache) {
		if (cache == null)
			return evaluate(event);
		//noinspection DataFlowIssue
		return cache.computeIfAbsent(this, (cond -> cond.evaluate(event)));
	}


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
		return and(other.evaluate(event, null), event, null);
	}

	/**
	 * Computes {@link Kleenean#and(Kleenean)} with the evaluations of this {@link Conditional} and the other.
	 * Evaluates the other first, and shortcuts if it is not {@link Kleenean#TRUE}.
	 * Uses a mutable cache of evaluated conditionals to prevent duplicate evaluations.
	 *
	 * @param other The {@link Conditional} to AND with. Will always be evaluated.
	 * @param event The event with which to evaluate the conditionals.
	 * @param cache The cache of evaluated conditionals.
	 * @return The result of {@link Kleenean#and(Kleenean)}, given the evaluations of the two conditionals.
	 */
	@Contract(pure = true)
	default Kleenean and(Conditional other, Event event, @Nullable Map<Conditional, Kleenean> cache) {
		return and(other.evaluate(event, cache), event, cache);
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
		return and(other, event, null);
	}

	/**
	 * Computes {@link Kleenean#and(Kleenean)} with the evaluation of this {@link Conditional} and the given Kleenean.
	 * Evaluates this object iff the given {@link Kleenean} is not {@link Kleenean#FALSE}.
	 * Uses a mutable cache of evaluated conditionals to prevent duplicate evaluations.
	 *
	 * @param other The {@link Kleenean} to AND with.
	 * @param event The event with which to evaluate the conditional, if necessary.
	 * @param cache The cache of evaluated conditionals.
	 * @return The result of {@link Kleenean#and(Kleenean)}, given the evaluation of the conditional.
	 */
	@Contract(pure = true)
	default Kleenean and(Kleenean other, Event event, @Nullable Map<Conditional, Kleenean> cache) {
		if (other.isFalse())
			return other;
		return other.and(evaluate(event, cache));
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
		return or(other.evaluate(event, null), event, null);
	}

	/**
	 * Computes {@link Kleenean#or(Kleenean)} with the evaluations of this {@link Conditional} and the other.
	 * Evaluates the other first, and shortcuts if it is not {@link Kleenean#TRUE}.
	 * Uses a mutable cache of evaluated conditionals to prevent duplicate evaluations.
	 *
	 * @param other The {@link Conditional} to OR with. Will always be evaluated.
	 * @param event The event with which to evaluate the conditionals.
	 * @param cache The cache of evaluated conditionals.
	 * @return The result of {@link Kleenean#and(Kleenean)}, given the evaluations of the two conditionals.
	 */
	@Contract(pure = true)
	default Kleenean or(Conditional other, Event event, @Nullable Map<Conditional, Kleenean> cache) {
		return or(other.evaluate(event, cache), event, cache);
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
		return or(other, event, null);
	}

	/**
	 * Computes {@link Kleenean#or(Kleenean)} with the evaluation of this {@link Conditional} and the given Kleenean.
	 * Evaluates this object iff the given {@link Kleenean} is {@link Kleenean#FALSE} or {@link Kleenean#UNKNOWN}.
	 * Uses a mutable cache of evaluated conditionals to prevent duplicate evaluations.
	 *
	 * @param other The {@link Kleenean} to OR with.
	 * @param event The event with which to evaluate the conditional, if necessary.
	 * @param cache The cache of evaluated conditionals.
	 * @return The result of {@link Kleenean#or(Kleenean)}, given the evaluation of the conditional.
	 */
	@Contract(pure = true)
	default Kleenean or(Kleenean other, Event event, @Nullable Map<Conditional, Kleenean> cache) {
		if (other.isTrue())
			return other;
		return other.or(evaluate(event, cache));
	}

	/**
	 * Computes {@link Kleenean#not()} on the evaluation of this {@link Conditional}.
	 * @param event The event with which to evaluate the conditional.
	 * @return The result of {@link Kleenean#not()}, given the evaluation of the conditional.
	 */
	@Contract(pure = true)
	default Kleenean not(Event event) {
		return not(event, null);
	}

	/**
	 * Computes {@link Kleenean#not()} on the evaluation of this {@link Conditional}.
	 * @param event The event with which to evaluate the conditional.
	 * @return The result of {@link Kleenean#not()}, given the evaluation of the conditional.
	 */
	@Contract(pure = true)
	default Kleenean not(Event event, @Nullable Map<Conditional, Kleenean> cache) {
		return this.evaluate(event, cache).not();
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

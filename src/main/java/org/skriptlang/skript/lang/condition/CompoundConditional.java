package org.skriptlang.skript.lang.condition;

import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A {@link Conditional} that is built of other {@link Conditional}s.
 * It is composed of a {@link List} of {@link Conditional}s that are acted upon by a single {@link Operator}.
 * @see ConditionalBuilder
 */
public class CompoundConditional implements Conditional {

	private final LinkedHashSet<Conditional> componentConditionals = new LinkedHashSet<>();
	private final Operator operator;

	/**
	 * Whether a cache will be used during evaluation. True if this compound contains other compounds, otherwise false.
	 */
	private boolean useCache;

	/**
	 * @param operator The {@link Operator} to use to combine the conditionals. If {@link Operator#NOT} is used,
	 * 					{@code conditionals} must exactly 1 conditional.
	 * @param conditionals A collection of conditionals to combine using the operator. Must be >= 1 in length,
	 *                     or exactly 1 if {@link Operator#NOT} is used.
	 */
	public CompoundConditional(Operator operator, @NotNull Collection<Conditional> conditionals) {
		if (conditionals.isEmpty())
			throw new IllegalArgumentException("CompoundConditionals must contain at least 1 component conditional.");
		if (operator == Operator.NOT && conditionals.size() != 1)
			throw new IllegalArgumentException("The NOT operator cannot be applied to multiple Conditionals.");

		this.componentConditionals.addAll(conditionals);
		useCache = conditionals.stream().anyMatch(cond -> cond instanceof CompoundConditional);
		this.operator = operator;
	}

	/**
	 * @param operator The {@link Operator} to use to combine the conditionals. If {@link Operator#NOT} is used,
	 * 					{@code conditionals} must exactly 1 conditional.
	 * @param conditionals Conditionals to combine using the operator. Must be >= 1 in length,
	 *                     or exactly 1 if {@link Operator#NOT} is used.
	 */
	public CompoundConditional(Operator operator, Conditional... conditionals) {
		this(operator, List.of(conditionals));
	}

	@Override
	public Kleenean evaluate(Event event) {
		Map<Conditional, Kleenean> cache = null;
		// only use overhead of a cache if we think it will be useful (stacked conditionals)
		if (useCache)
			cache = new HashMap<>();
		return evaluate(event, cache);
	}

	@Override
	public Kleenean evaluate(Event event, Map<Conditional, Kleenean> cache) {
		Kleenean result;
		return switch (operator) {
			case OR -> {
				result = Kleenean.FALSE;
				for (Conditional conditional : componentConditionals) {
					result = conditional.or(result, event, cache);
				}
				yield result;
			}
			case AND -> {
				result = Kleenean.TRUE;
				for (Conditional conditional : componentConditionals) {
					result = conditional.and(result, event, cache);
				}
				yield result;
			}
			case NOT -> {
				if (componentConditionals.size() > 1)
					throw new IllegalStateException("Cannot apply NOT to multiple conditionals! Cannot evaluate.");
				yield componentConditionals.getFirst().evaluate(event, cache);
			}
		};
	}

	/**
	 * @return An immutable list of the component conditionals of this object.
	 */
	public List<Conditional> getConditionals() {
		return componentConditionals.stream().toList();
	}

	/**
	 * @return The operator used in this object.
	 */
	public Operator getOperator() {
		return operator;
	}

	/**
	 * @param conditionals Adds more conditionals to this object's component conditionals.
	 */
	protected void addConditionals(Conditional... conditionals) {
		addConditionals(List.of(conditionals));
	}

	/**
	 * @param conditionals Adds more conditionals to this object's component conditionals.
	 */
	protected void addConditionals(Collection<Conditional> conditionals) {
		componentConditionals.addAll(conditionals);
		useCache |= conditionals.stream().anyMatch(cond -> cond instanceof CompoundConditional);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "(" +
			componentConditionals.stream()
				.map(conditional -> conditional.toString(event, debug))
				.collect(Collectors.joining(" " + operator.getSymbol() + " ")) +
			")";
	}

	@Override
	public String toString() {
		return toString(null, false);
	}
}

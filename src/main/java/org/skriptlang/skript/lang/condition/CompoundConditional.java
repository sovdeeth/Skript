package org.skriptlang.skript.lang.condition;

import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * A {@link Conditional} that is built of other {@link Conditional}s.
 * It is composed of a {@link List} of {@link Conditional}s that are acted upon by a single {@link Operator}.
 * A {@link CompoundConditional} can be constructed using the {@link ConditionalBuilder} and will be
 * simplified into CNF form when built.
 */
public class CompoundConditional implements Conditional {

	private final LinkedHashSet<Conditional> componentConditionals = new LinkedHashSet<>();
	private final Operator operator;
	private boolean useCache;

	public CompoundConditional(Operator operator, @NotNull Collection<Conditional> conditionals) {
		if (conditionals.isEmpty())
			throw new IllegalArgumentException("CompoundConditionals must contain at least 1 component conditional.");
		if (operator == Operator.NOT && conditionals.size() != 1)
			throw new IllegalArgumentException("The NOT operator cannot be applied to multiple Conditionals.");
		this.componentConditionals.addAll(conditionals);
		useCache = conditionals.stream().anyMatch(cond -> cond instanceof CompoundConditional);
		this.operator = operator;
	}

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

	public List<Conditional> getConditionals() {
		return componentConditionals.stream().toList();
	}

	public Operator getOperator() {
		return operator;
	}

	protected void addConditionals(Conditional... conditionals) {
		addConditionals(List.of(conditionals));
	}

	protected void addConditionals(Collection<Conditional> conditionals) {
		componentConditionals.addAll(conditionals);
		useCache |= conditionals.stream().anyMatch(cond -> cond instanceof CompoundConditional);
	}
}

package org.skriptlang.skript.lang.condition;

import ch.njol.util.Kleenean;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link Conditional} that is built of other {@link Conditional}s.
 * It is composed of a {@link List} of {@link Conditional}s that are acted upon by a single {@link Operator}.
 * A {@link CompoundConditional} can be constructed using the {@link ConditionalBuilder} and will be
 * simplified into CNF form when built.
 */
public class CompoundConditional implements Conditional {

	private final List<Conditional> componentCondtionals = new ArrayList<>();
	private final Operator operator;

	public CompoundConditional(Operator operator, List<Conditional> conditionals) {
		if (conditionals.isEmpty())
			throw new IllegalArgumentException("CompoundConditionals must contain at least 1 component conditional.");
		if (operator == Operator.NOT && conditionals.size() != 1)
			throw new IllegalArgumentException("The NOT operator cannot be applied to multiple Conditionals.");
		this.componentCondtionals.addAll(conditionals);
		this.operator = operator;
	}

	public CompoundConditional(Operator operator, Conditional... conditionals) {
		this(operator, List.of(conditionals));
	}

	@Override
	public Kleenean evaluate(Event event) {
		Kleenean result;
		return switch (operator) {
			case OR -> {
				result = Kleenean.FALSE;
				for (Conditional conditional : componentCondtionals) {
					result = conditional.or(result, event);
				}
				yield result;
			}
			case AND -> {
				result = Kleenean.TRUE;
				for (Conditional conditional : componentCondtionals) {
					result = conditional.and(result, event);
				}
				yield result;
			}
			case NOT -> {
				if (componentCondtionals.size() > 1)
					throw new IllegalStateException("Cannot apply NOT to multiple conditionals! Cannot evaluate.");
				yield componentCondtionals.getFirst().evaluate(event);
			}
		};
	}

	public List<Conditional> getCondtionals() {
		return Collections.unmodifiableList(componentCondtionals);
	}

	public Operator getOperator() {
		return operator;
	}

	protected void addConditionals(Conditional... conditionals) {
		addConditionals(List.of(conditionals));
	}

	protected void addConditionals(List<Conditional> conditionals) {
		componentCondtionals.addAll(conditionals);
	}
}

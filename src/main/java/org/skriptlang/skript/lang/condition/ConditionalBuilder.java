package org.skriptlang.skript.lang.condition;

import org.skriptlang.skript.lang.condition.Conditional.Operator;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a DNF {@link CompoundConditional}, meaning it is solely composed of groups of ANDs all ORed together,
 * ex. {@code (a && !b && c) || b || (!c && d)}.
 * <br>
 * A builder should no longer be used after calling {@link #build()}.
 */
public class ConditionalBuilder {

	private CompoundConditional root;

	protected ConditionalBuilder() {
		this.root = null;
	}

	public Conditional build() {
		if (root == null)
			throw new IllegalStateException("Cannot build an empty conditional!");
		return root;
	}

	// (existing) && newA && newB && ...
	public ConditionalBuilder and(Conditional... andCondtionals) {
		if (root == null) {
			root = new CompoundConditional(Operator.AND, andCondtionals);
			return this;
		}

		// if the root is still just AND, we can just append.
		if (root.getOperator() == Operator.AND) {
			root.addConditionals(andCondtionals);
			return this;
		}
		// Otherwise, we need to transform:
		// (a || b) && c -> (a && c) || (b && c)
		List<Conditional> newConditionals = new ArrayList<>();
		List<Conditional> inputConditionals = new ArrayList<>(andCondtionals.length + 1);
		inputConditionals.set(0, null); // just for padding
		inputConditionals.addAll(List.of(andCondtionals));
		for (Conditional conditional : root.getConditionals()) {
			inputConditionals.set(0, conditional);
			newConditionals.add(new CompoundConditional(Operator.AND, inputConditionals));
		}

		root = new CompoundConditional(Operator.OR, newConditionals);
		return this;
	}

	// (existing) || newA || newB || ...
	public ConditionalBuilder or(Conditional... orCondtionals) {
		if (root == null) {
			root = new CompoundConditional(Operator.OR, orCondtionals);
			return this;
		}

		// Since DNF is a series of ANDs, ORed together, we can simply add these to the root if it's already OR.
		if (root.getOperator() == Operator.OR) {
			root.addConditionals(orCondtionals);
			return this;
		}
		// otherwise we need to nest the AND/NOT condition within a new root with OR operator.
		List<Conditional> conditionalList = new ArrayList<>();
		conditionalList.add(0, root);
		conditionalList.addAll(List.of(orCondtionals));
		root = new CompoundConditional(Operator.OR, conditionalList);
		return this;
	}

	// (existing) && !(new)
	public ConditionalBuilder andNot(Conditional conditional) {
		return and(negate(conditional));
	}

	// (existing) || !(new)
	public ConditionalBuilder orNot(Conditional conditional) {
		return or(negate(conditional));
	}

	public ConditionalBuilder add(boolean or, Conditional... conditionals) {
		if (or)
			return or(conditionals);
		return and(conditionals);
	}

	private static Conditional negate(Conditional condtional) {
		if (!(condtional instanceof CompoundConditional compound))
			return new CompoundConditional(Operator.NOT, condtional);

		return switch (compound.getOperator()) {
			// !!a -> a
			case NOT -> compound.getConditionals().getFirst();
			// !(a && b) -> (!a || !b)
			case AND -> {
				List<Conditional> newConditionals = new ArrayList<>();
				for (Conditional cond : compound.getConditionals()) {
					newConditionals.add(negate(cond));
				}
				yield new CompoundConditional(Operator.OR, newConditionals);
			}
			// !(a || b) -> (!a && !b)
			case OR -> {
				List<Conditional> newConditionals = new ArrayList<>();
				for (Conditional cond : compound.getConditionals()) {
					newConditionals.add(negate(cond));
				}
				yield new CompoundConditional(Operator.AND, newConditionals);
			}
		};
	}

}

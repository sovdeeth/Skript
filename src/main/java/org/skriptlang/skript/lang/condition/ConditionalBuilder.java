package org.skriptlang.skript.lang.condition;

import org.jetbrains.annotations.Contract;
import org.skriptlang.skript.lang.condition.Conditional.Operator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.skriptlang.skript.lang.condition.Conditional.negate;

/**
 * Builds a DNF {@link CompoundConditional}, meaning it is solely composed of groups of ANDs all ORed together,
 * ex. {@code (a && !b && c) || b || (!c && d)}.
 * <br>
 * A builder should no longer be used after calling {@link #build()}.
 * @see CompoundConditional
 */
public class ConditionalBuilder {

	private CompoundConditional root;

	/**
	 * Creates an empty builder.
	 */
	protected ConditionalBuilder() {
		this.root = null;
	}

	/**
	 * Creates a builder with a single conditional.
	 * @param root If given a {@link CompoundConditional}, it is used as the root conditional.
	 *             Otherwise, a OR compound conditional is created as the root with the given conditional within.
	 */
	protected ConditionalBuilder(Conditional root) {
		if (root instanceof CompoundConditional compoundConditional) {
			this.root = compoundConditional;
		} else {
			this.root = new CompoundConditional(Operator.OR, root);
		}
	}

	/**
	 * @return The root conditional, which will be DNF-compliant
	 * @throws IllegalStateException if the builder is empty.
	 */
	public Conditional build() {
		if (root == null)
			throw new IllegalStateException("Cannot build an empty conditional!");
		return root;
	}

	/**
	 * Adds conditionals to the root node via the AND operator: {@code (existing) && newA && newB && ...}.
	 * If the root is currently OR, the statement is transformed as follows to maintain DNF:
	 * {@code (a || b) && c -> (a && c) || (b && c)}
	 * @param andConditionals conditionals to AND to the existing conditional.
	 * @return the builder
	 */
	@Contract("_ -> this")
	public ConditionalBuilder and(Conditional... andConditionals) {
		if (root == null) {
			root = new CompoundConditional(Operator.AND, andConditionals);
			return this;
		}

		// unroll conditionals if they're ANDs
		List<Conditional> newConditionals = unroll(List.of(andConditionals), Operator.AND);

		// if the root is still just AND, we can just append.
		if (root.getOperator() == Operator.AND) {
			root.addConditionals(newConditionals);
			return this;
		}
		// Otherwise, we need to transform:
		// (a || b) && c -> (a && c) || (b && c)
		List<Conditional> transformedConditionals = new ArrayList<>();
		newConditionals.add(0, null); // just for padding
		for (Conditional conditional : root.getConditionals()) {
			newConditionals.set(0, conditional);
			transformedConditionals.add(new CompoundConditional(Operator.AND, newConditionals));
		}

		root = new CompoundConditional(Operator.OR, transformedConditionals);
		return this;
	}

	/**
	 * Adds conditionals to the root node via the OR operator: {@code (existing) || newA || newB || ...}.
	 * If the root is currently AND, a new OR root node is created containing the previous root and the new conditionals.
	 * @param orConditionals conditionals to OR to the existing conditional.
	 * @return the builder
	 */
	@Contract("_ -> this")
	public ConditionalBuilder or(Conditional... orConditionals) {
		if (root == null) {
			root = new CompoundConditional(Operator.OR, orConditionals);
			return this;
		}

		// unroll conditionals if they're ORs
		List<Conditional> newConditionals = unroll(List.of(orConditionals), Operator.OR);

		// Since DNF is a series of ANDs, ORed together, we can simply add these to the root if it's already OR.
		if (root.getOperator() == Operator.OR) {
			root.addConditionals(newConditionals);
			return this;
		}
		// otherwise we need to nest the AND/NOT condition within a new root with OR operator.
		newConditionals.add(0, root);
		root = new CompoundConditional(Operator.OR, newConditionals);
		return this;
	}

	/**
	 * Unrolls nested conditionals which are superfluous:
	 * {@code a || (b || c) -> a || b || c}
	 * @param conditionals A collection of conditionals to unroll.
	 * @param operator Which operator to unroll.
	 * @return A new list of conditionals without superfluous nesting.
	 */
	@Contract("_,_ -> new")
	private static List<Conditional> unroll(Collection<Conditional> conditionals, Operator operator) {
		List<Conditional> newConditionals = new ArrayList<>();
		for (Conditional conditional : conditionals) {
			if (conditional instanceof CompoundConditional compound && compound.getOperator() == operator) {
				newConditionals.addAll(unroll(compound.getConditionals(), operator));
			} else {
				newConditionals.add(conditional);
			}
		}
		return newConditionals;
	}

	/**
	 * Adds a negated conditional to the root node via the AND and NOT operators: {@code (existing) && !new}.
	 * @param conditional The conditional to negate and add.
	 * @return the builder
	 */
	@Contract("_ -> this")
	public ConditionalBuilder andNot(Conditional conditional) {
		return and(negate(conditional));
	}

	/**
	 * Adds a negated conditional to the root node via the OR and NOT operators: {@code (existing) || !new}.
	 * @param conditional The conditional to negate and add.
	 * @return the builder
	 */
	@Contract("_ -> this")
	public ConditionalBuilder orNot(Conditional conditional) {
		return or(negate(conditional));
	}

	/**
	 * Adds conditionals to the root node via the AND or OR operators.
	 * A helper for dynamically adding conditionals.
	 * @param or Whether to use OR (true) or AND (false)
	 * @param conditionals The conditional to add.
	 * @return the builder
	 */
	@Contract("_,_ -> this")
	public ConditionalBuilder add(boolean or, Conditional... conditionals) {
		if (or)
			return or(conditionals);
		return and(conditionals);
	}

}

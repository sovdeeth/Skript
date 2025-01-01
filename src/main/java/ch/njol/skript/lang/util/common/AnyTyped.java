package ch.njol.skript.lang.util.common;

import org.jetbrains.annotations.UnknownNullability;

/**
 * A provider for anything with a type.
 * Anything implementing this (or convertible to this) can be used by the {@link ch.njol.skript.expressions.ExprTypeOf}
 * property expression.
 *
 * @param <T> The class that this type is. (e.g. EntityType for Entities)
 *
 * @see AnyProvider
 */
@FunctionalInterface
public interface AnyTyped<T> extends AnyProvider {

	/**
	 * @return This thing's type
	 */
	@UnknownNullability
	T type();

}

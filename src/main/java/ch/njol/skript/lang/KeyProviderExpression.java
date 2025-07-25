package ch.njol.skript.lang;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.util.coll.iterator.ArrayIterator;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * Represents an expression that is able to return a set of keys linked to its values.
 * This can be used to return index-linked values to store in a list variable,
 * using the {@link ChangeMode#SET} {@link Changer} or passed to a function argument.
 * An expression can provide a set of keys to use, rather than numerical indices.
 * <br/>
 * Index-linking is not (currently) used with other change modes.
 * <br/>
 * <br/>
 * <h2>Contract</h2>
 * <ul>
 *     <li>Neither {@link #getArrayKeys(Event)} nor {@link #getAllKeys(Event)} should ever be called without
 *     a corresponding {@link #getArray(Event)} or {@link #getAll(Event)} call.</li>
 *     <li>{@link #getArrayKeys(Event)} and {@link #getAllKeys(Event)} should only be called iff {@link #canReturnKeys()}
 *     returns {@code true}.</li>
 *     <li>A caller may ask only for values and does not have to invoke either {@link #getArrayKeys(Event)} or
 *     {@link #getAllKeys(Event)}.</li>
 *     <li>{@link #getArrayKeys(Event)} might be called after the corresponding {@link #getArray(Event)}</li>
 *     <li>{@link #getAllKeys(Event)} might be called after the corresponding {@link #getAll(Event)}</li>
 * </ul>
 * <br/>
 * <h2>Advice on Caching</h2>
 * As long as callers are built sensibly and follow API advice, it should be safe to cache a key-list during a values
 * call.
 * E.g. if an expression is returning data from a map, it could request the whole entry-set during
 * {@link #getArray(Event)}
 * and return the keys during {@link #getArrayKeys(Event)} (provided the cache is weak, safe and event-linked).
 * This is not necessary, but it may be helpful for some expressions where the set of keys could potentially change
 * between repeated calls, or is expensive to access.
 * <br/>
 * <br/>
 * <h3>Caveats</h3>
 * <ol>
 *     <li>The caller may <i>never</i> ask for {@link #getArrayKeys(Event)}.
 *     The cache should be disposed of in a timely manner.</li>
 *     <li>It is (theoretically) possible for two separate calls to occur simultaneously
 *     (asking for the value/key sets separately) so it is recommended to link any cache system to the event instance
 *     .</li>
 * </ol>
 * Note that the caller may <i>never</i> ask for {@link #getArrayKeys(Event)} and so the cache should be disposed of
 * in a timely manner.
 * <br/>
 * <pre>{@code
 * Map<Event, Collection<String>> cache = new WeakHashMap<>();
 *
 * public Object[] getArray(Event event) {
 *     Set<Entry<String, T>> entries = something.entrySet();
 *     cache.put(event, List.copyOf(something.keySet()));
 *     return something.values().toArray(...);
 * }
 *
 * public String[] getArrayKeys(Event event) {
 *     if (!cache.containsKey(event))
 *         throw new IllegalStateException();
 *     return cache.remove(event).toArray(new String[0]);
 *     // this should never be absent/null
 * }
 * }</pre>
 *
 * @see Expression
 * @see KeyReceiverExpression
 * @see KeyedValue
 */
public interface KeyProviderExpression<T> extends Expression<T> {

	/**
	 * A set of keys, matching the length and order of the immediately-previous
	 * {@link #getArray(Event)} values array.
	 * <br/>
	 * This should <b>only</b> be called immediately after a {@link #getArray(Event)} invocation,
	 * and iff {@link #canReturnKeys()} returns {@code true}.
	 * If it is called without a matching values request (or after a delay) then the behaviour
	 * is undefined, in which case:
	 * <ul>
	 *     <li>the method may throw an error,</li>
	 *     <li>the method may return keys not matching any previous values,</li>
	 *     <li>or the method may return nothing at all.</li>
	 * </ul>
	 *
	 * @param event The event context
	 * @return A set of keys, of the same length as {@link #getArray(Event)}
	 * @throws IllegalStateException If this was not called directly after a {@link #getArray(Event)} call
	 * or if {@link #canReturnKeys()} returns {@code false}
	 */
	@NotNull String @NotNull [] getArrayKeys(Event event) throws IllegalStateException;

	/**
	 * A set of keys, matching the length and order of the immediately-previous
	 * {@link #getAll(Event)} values array.
	 * <br/>
	 * This should <b>only</b> be called immediately after a {@link #getAll(Event)} invocation,
	 * and iff {@link #canReturnKeys()} returns {@code true}.
	 * If it is called without a matching values request (or after a delay) then the behaviour
	 * is undefined, in which case:
	 * <ul>
	 *     <li>the method may throw an error,</li>
	 *     <li>the method may return keys not matching any previous values,</li>
	 *     <li>or the method may return nothing at all.</li>
	 * </ul>
	 *
	 * @param event The event context
	 * @return A set of keys, of the same length as {@link #getAll(Event)}
	 * @throws IllegalStateException If this was not called directly after a {@link #getAll(Event)} call
	 * or if {@link #canReturnKeys()} returns {@code false}
	 */
	default @NotNull String @NotNull [] getAllKeys(Event event) {
		return this.getArrayKeys(event);
	}

	/**
	 * Returns an iterator over the keyed values of this expression.
	 * <br/>
	 * This should <b>only</b> be called iff {@link #canReturnKeys()} returns {@code true}.
	 *
	 * @param event The event context
	 * @return An iterator over the key-value pairs of this expression
	 */
	default Iterator<KeyedValue<T>> keyedIterator(Event event) {
		return new ArrayIterator<>(KeyedValue.zip(getArray(event), getArrayKeys(event)));
	}

	/**
	 * Keyed expressions should never be single.
	 */
	@Override
	default boolean isSingle() {
		return false;
	}

	/**
	 * Returns whether this expression can return keys.
	 * <br/>
	 * If this returns false, then {@link #getArrayKeys(Event)} and {@link #getAllKeys(Event)} should never be called.
	 *
	 * @return Whether this expression can return keys
	 */
	default boolean canReturnKeys() {
		return true;
	}

	/**
	 * While all keyed expressions may <i>offer</i> their keys,
	 * some may prefer that they are not used unless strictly required (e.g. variables).
	 *
	 * @return Whether the caller is recommended to ask for keys after asking for values
	 */
	default boolean areKeysRecommended() {
		return true;
	}

	@Override
	default boolean isLoopOf(String input) {
		return canReturnKeys() && isIndexLoop(input);
	}


	/**
	 * Checks whether the 'loop-...' expression should match this loop's index,
	 * e.g. loop-index matches the index of a loop that iterates over a list variable.
	 *
	 * @param input the input to check
	 * @return true if the input matches the index loop, false otherwise
	 */
	default boolean isIndexLoop(String input) {
		return input.equalsIgnoreCase("index");
	}

	/**
	 * Checks if the given expression can return keys.
	 *
	 * @param expression the expression to check
	 * @return true if the expression can return keys, false otherwise
	 * @see #canReturnKeys()
	 */
	static boolean canReturnKeys(Expression<?> expression) {
		return expression instanceof KeyProviderExpression<?> provider && provider.canReturnKeys();
	}

	/**
	 * Checks if the given expression can return keys and whether it is recommended to use them.
	 *
	 * @param expression the expression to check
	 * @return true if the expression can return keys, and it is recommended to use them, false otherwise
	 * @see #areKeysRecommended()
	 * @see #canReturnKeys()
	 */
	static boolean areKeysRecommended(Expression<?> expression) {
		return canReturnKeys(expression) && ((KeyProviderExpression<?>) expression).areKeysRecommended();
	}

}

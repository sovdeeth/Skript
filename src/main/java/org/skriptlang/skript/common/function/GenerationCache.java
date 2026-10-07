package org.skriptlang.skript.common.function;

import ch.njol.skript.lang.function.FunctionRegistry;
import com.google.common.base.Preconditions;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A cache of things worked out from the registered functions, discarded whenever those change.
 * <p>
 * Anything derived from the function registry — which overload a name resolves to, which signature
 * a set of arguments binds to — stops being true as soon as a function is registered or removed,
 * which a script being reloaded does. Rather than each cache checking
 * {@link FunctionRegistry#generation()} and deciding for itself what to discard, they all go
 * through this: the generation is read once per lookup, and the whole cache is dropped when it has
 * moved.
 * </p>
 *
 * @param <K> The key type.
 * @param <V> The cached value type.
 */
@ApiStatus.Internal
public final class GenerationCache<K, V> {

	/**
	 * How many entries a bounded cache holds before it stops taking new ones. Reaching this is not
	 * expected; it is only there so that a key built at runtime cannot grow the cache without
	 * bound.
	 */
	private final int capacity;

	private final Map<K, V> entries = new ConcurrentHashMap<>();

	/**
	 * The generation {@link #entries} was filled against. Deliberately initialised to a value the
	 * registry cannot be at, so that the first lookup does not have to be a special case.
	 */
	private volatile long generation = Long.MIN_VALUE;

	private GenerationCache(int capacity) {
		this.capacity = capacity;
	}

	/**
	 * @param capacity How many entries to hold at most. Must be positive.
	 * @return A cache which stops taking new entries once it holds {@code capacity} of them.
	 */
	public static <K, V> @NotNull GenerationCache<K, V> bounded(int capacity) {
		Preconditions.checkArgument(capacity > 0, "capacity must be positive");
		return new GenerationCache<>(capacity);
	}

	/**
	 * @return A cache with no limit on how many entries it holds, for keys which cannot be built at
	 * 	runtime and are therefore already bounded by how much is loaded.
	 */
	public static <K, V> @NotNull GenerationCache<K, V> unbounded() {
		return new GenerationCache<>(Integer.MAX_VALUE);
	}

	/**
	 * Returns the value cached for {@code key}, working it out if it is not cached.
	 * <p>
	 * A {@code compute} which returns null caches nothing, so that a key which resolves to no
	 * function is worked out again rather than stored as an absence.
	 * </p>
	 *
	 * @param key     The key.
	 * @param compute How to work out the value, called only if it is not already cached.
	 * @return The value, or null if {@code compute} returned null.
	 */
	public @Nullable V get(@NotNull K key, @NotNull Function<? super K, ? extends V> compute) {
		discardIfStale();

		V existing = entries.get(key);
		if (existing != null) {
			return existing;
		}

		// at capacity the value is worked out but not kept, so that the entries already held --
		// which are the ones this has seen most -- keep working. Clearing instead would mean a
		// cache which never helps again once it has once been overfilled
		if (entries.size() >= capacity) {
			return compute.apply(key);
		}

		return entries.computeIfAbsent(key, compute);
	}

	/**
	 * Drops everything if the registered functions have changed since this was filled.
	 * <p>
	 * Reading the generation before the value is worked out is what makes this safe: the registry
	 * moves its generation only once it has changed, so a change landing while a value is being
	 * worked out leaves this behind the current generation and the entry is dropped on the next
	 * lookup rather than kept.
	 * </p>
	 */
	private void discardIfStale() {
		long current = FunctionRegistry.getRegistry().generation();
		if (generation == current) {
			return;
		}

		// two threads may both find it stale and both clear, which costs a little work but cannot
		// leave an entry from an older generation behind
		entries.clear();
		generation = current;
	}

}

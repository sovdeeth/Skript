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
 * through this: each entry carries the generation it was worked out against, and an entry whose
 * generation has moved is worked out again rather than returned.
 * </p>
 *
 * @param <K> The key type.
 * @param <V> The cached value type.
 */
@ApiStatus.Internal
public final class GenerationCache<K, V> {

	/**
	 * How many entries this holds before it stops taking new ones. Reaching it is not expected; it
	 * is there so that keys which keep arriving -- a name built at runtime, or a call site parsed
	 * again on every reload -- cannot grow the cache without bound.
	 */
	private final int capacity;

	private final Map<K, Entry<V>> entries = new ConcurrentHashMap<>();

	/**
	 * The generation {@link #entries} was last emptied at. Only used to drop entries nothing will
	 * ask for again; correctness rests on the generation stamped on each entry, not on this.
	 */
	private volatile long generation = Long.MIN_VALUE;

	/**
	 * A cached value together with the generation it was worked out against.
	 * <p>
	 * Stamped per entry rather than per cache because the cache is read concurrently: one thread
	 * may be part way through working a value out when another finds the cache stale and empties
	 * it, and a single generation for the whole cache would then describe that value wrongly.
	 * </p>
	 *
	 * @param value      The cached value.
	 * @param generation The {@link FunctionRegistry#generation()} it was worked out against.
	 */
	private record Entry<V>(V value, long generation) {

	}

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
	 * Returns the value cached for {@code key}, working it out if it is not cached or was worked
	 * out against overloads which have since changed.
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
		// read before the value is worked out, so that a change landing while it is being worked
		// out stamps the entry with the generation from before the change. The next lookup reads
		// the newer generation, does not match the stamp, and works the value out again
		long current = FunctionRegistry.getRegistry().generation();
		discardStale(current);

		Entry<V> existing = entries.get(key);
		if (existing != null && existing.generation() == current) {
			return existing.value();
		}

		// at capacity the value is worked out but not kept, so that the entries already held --
		// which are the ones this has seen most -- keep working. Emptying it instead would mean a
		// cache which never helps again once it has once been overfilled. Refreshing a key already
		// held does not grow it, so only a key it has never seen is turned away
		if (existing == null && entries.size() >= capacity) {
			return compute.apply(key);
		}

		// one mapping per key even under concurrent lookups, which a call site depends on: the
		// spread memo it carries is only shared if every caller is handed the same object
		Entry<V> updated = entries.compute(key, (ignored, present) -> {
			if (present != null && present.generation() == current) {
				return present; // another thread refreshed it first
			}

			V computed = compute.apply(key);
			return computed != null ? new Entry<>(computed, current) : null;
		});

		return updated != null ? updated.value() : null;
	}

	/**
	 * Empties the cache if the registered functions have changed since it was last emptied.
	 * <p>
	 * This is housekeeping, not correctness: every entry is checked against the generation it was
	 * worked out under, so an entry stored just after this runs is caught by its own stamp. Without
	 * it the cache would keep entries nothing will ask for again until it reached its capacity.
	 * </p>
	 *
	 * @param current The generation the registry is at.
	 */
	private void discardStale(long current) {
		if (generation == current) {
			return;
		}

		entries.clear();
		generation = current;
	}

}

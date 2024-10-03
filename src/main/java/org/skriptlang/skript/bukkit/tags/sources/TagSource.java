package org.skriptlang.skript.bukkit.tags.sources;

import ch.njol.util.coll.iterator.CheckedIterator;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.tags.TagType;

import java.util.Iterator;
import java.util.function.Predicate;

/**
 * A source for {@link org.bukkit.Tag}s, be it Bukkit's tag registries, Paper's handmade tags, or
 * custom tags made by the user.
 * @param <T> The type of tags this source will return.
 *           For example, the Bukkit "items" tag registry would return {@link org.bukkit.Material}s.
 */
public abstract class TagSource<T extends Keyed> {

	private final TagType<T>[] types;

	@SafeVarargs
	protected TagSource(TagType<T>... types) {
		this.types = types;
	}

	/**
	 * @return All the tags associated with this source.
	 */
	public abstract Iterable<Tag<T>> getAllTags();

	public Iterable<Tag<T>> getAllTagsMatching(Predicate<Tag<T>> predicate) {
		Iterator<Tag<T>> tagIterator = getAllTags().iterator();
 		return new Iterable<>() {
			@Override
			public @NotNull Iterator<Tag<T>> iterator() {
				return new CheckedIterator<>(tagIterator, predicate::test);
			}
		};
	}

	public abstract @Nullable Tag<T> getTag(NamespacedKey key);

	public TagType<T>[] getTypes() {
		return types;
	}



}

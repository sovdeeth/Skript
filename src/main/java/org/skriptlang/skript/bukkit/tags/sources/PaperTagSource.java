package org.skriptlang.skript.bukkit.tags.sources;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.bukkit.tags.TagType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PaperTagSource<T extends Keyed> extends CustomTagSource<T> {

	private static <T extends Keyed> @NotNull Iterable<Tag<T>> getPaperTags(@NotNull Iterable<Tag<T>> tags) {
		List<Tag<T>> modified_tags = new ArrayList<>();
		for (Tag<T> tag : tags) {
			modified_tags.add(new PaperTag<>(tag));
		}
		return modified_tags;
	}

	@SafeVarargs
	public PaperTagSource(Iterable<Tag<T>> tags, TagType<T>... types) {
		super(getPaperTags(tags), types);
	}

	/**
	 * Wrapper for Paper tags to remove "_settag" from their key.
	 * @param <T1>
	 */
	private static class PaperTag<T1 extends Keyed> implements Tag<T1> {

		private final Tag<T1> paperTag;
		private final NamespacedKey key;

		public PaperTag(@NotNull Tag<T1> paperTag) {
			this.paperTag = paperTag;
			this.key = NamespacedKey.fromString(paperTag.getKey().toString().replace("_settag", ""));
		}

		@Override
		public boolean isTagged(@NotNull T1 item) {
			return paperTag.isTagged(item);
		}

		@Override
		public @NotNull Set<T1> getValues() {
			return paperTag.getValues();
		}

		@Override
		public @NotNull NamespacedKey getKey() {
			return key;
		}
	}

}

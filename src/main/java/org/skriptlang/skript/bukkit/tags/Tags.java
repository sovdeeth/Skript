package org.skriptlang.skript.bukkit.tags;

import ch.njol.util.coll.iterator.CheckedIterator;
import com.destroystokyo.paper.MaterialTags;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterators;
import io.papermc.paper.tag.EntityTags;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.tags.sources.BukkitTagSource;
import org.skriptlang.skript.bukkit.tags.sources.PaperTagSource;
import org.skriptlang.skript.bukkit.tags.sources.TagOrigin;
import org.skriptlang.skript.bukkit.tags.sources.TagSource;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class Tags {

	private final TagSourceMap tagSourceMap = new TagSourceMap();

	public Tags() {
		tagSourceMap.put(TagType.ITEMS, new BukkitTagSource<>("items", TagType.ITEMS));
		tagSourceMap.put(TagType.BLOCKS, new BukkitTagSource<>("blocks", TagType.BLOCKS));
		tagSourceMap.put(TagType.ENTITIES, new BukkitTagSource<>("entity_types", TagType.ENTITIES));

		if (TagModule.PAPER_TAGS_EXIST) {
			try {
				List<Tag<Material>> materialTags = new ArrayList<>();
				for (Field field : MaterialTags.class.getDeclaredFields()) {
					if (field.canAccess(null))
						//noinspection unchecked
						materialTags.add((Tag<Material>) field.get(null));
				}
				PaperTagSource<Material> paperMaterialTags = new PaperTagSource<>(materialTags, TagType.BLOCKS, TagType.ITEMS);
				tagSourceMap.put(TagType.BLOCKS, paperMaterialTags);
				tagSourceMap.put(TagType.ITEMS, paperMaterialTags);

				List<Tag<EntityType>> entityTags = new ArrayList<>();
				for (Field field : EntityTags.class.getDeclaredFields()) {
					if (field.canAccess(null))
						//noinspection unchecked
						entityTags.add((Tag<EntityType>) field.get(null));
				}
				PaperTagSource<EntityType> paperEntityTags = new PaperTagSource<>(entityTags, TagType.ENTITIES);
				tagSourceMap.put(TagType.ENTITIES, paperEntityTags);
			} catch (IllegalAccessException ignored) {}
		}
	}

	public <T extends Keyed>  Iterable<Tag<T>> getTags(TagOrigin origin, Class<T> typeClass) {
		List<Iterator<Tag<T>>> tagIterators = new ArrayList<>();
		for (TagType<?> type : tagSourceMap.map.keys()) {
			if (type.type() == typeClass)
				//noinspection unchecked
				tagIterators.add(getTags(origin, (TagType<T>) type).iterator());
		}
		return new Iterable<>() {
			@Override
			public @NotNull Iterator<Tag<T>> iterator() {
				return Iterators.concat(tagIterators.iterator());
			}
		};
	}

	public <T extends Keyed> Iterable<Tag<T>> getTags(TagOrigin origin, TagType<T> type) {
		if (!tagSourceMap.containsKey(type))
			return List.of();
		Iterator<TagSource<T>> tagSources = tagSourceMap.get(origin, type).iterator();
		if (!tagSources.hasNext())
			return List.of();
		return new Iterable<>() {
			@Override
			public @NotNull Iterator<Tag<T>> iterator() {
				return new Iterator<>() {
					//<editor-fold desc="iterator over tagSources, returning each individual tag">
					private Iterator<Tag<T>> currentTagIter = tagSources.next().getAllTags().iterator();
					private Iterator<Tag<T>> nextTagIter;

					@Override
					public boolean hasNext() {
						// does the current source have more tags
						if (currentTagIter.hasNext())
							return true;
						// is there another source in the pipeline? if so, check it.
						if (nextTagIter != null)
							return nextTagIter.hasNext();
						// if there's no known next source, check if have one.
						// if we do, mark it as the next source.
						if (tagSources.hasNext()) {
							nextTagIter = tagSources.next().getAllTags().iterator();
							return nextTagIter.hasNext();
						}
						return false;
					}

					@Override
					public Tag<T> next() {
						// if current source has more, get more.
						if (currentTagIter.hasNext())
							return currentTagIter.next();
						// if current source is dry, switch to using the next source
						if (nextTagIter != null && nextTagIter.hasNext()) {
							currentTagIter = nextTagIter;
							nextTagIter = null;
							return currentTagIter.next();
						}
						throw new IllegalStateException("Called next without calling hasNext to set the next tag iterator.");
					}
					//</editor-fold>
				};
			}
		};
	}

	public <T extends Keyed> Iterable<Tag<T>> getTagsMatching(TagOrigin origin, TagType<T> type, Predicate<Tag<T>> predicate) {
		Iterator<Tag<T>> tagIterator = getTags(origin, type).iterator();
		return new Iterable<>() {
			@Override
			public @NotNull Iterator<Tag<T>> iterator() {
				return new CheckedIterator<>(tagIterator, predicate::test);
			}
		};
	}

	public <T extends Keyed> @Nullable Tag<T> getTag(TagOrigin origin, TagType<T> type, NamespacedKey key) {
		Tag<T> tag;
		for (TagSource<T> source : tagSourceMap.get(origin, type)) {
			tag = source.getTag(key);
			if (tag != null)
				return tag;
		}
		return null;
	}

	private static class TagSourceMap {
		private final ArrayListMultimap<TagType<?>, TagSource<?>> map = ArrayListMultimap.create();

		public <T extends Keyed> void put(TagType<T> key, TagSource<T> value) {
			map.put(key, value);
		}

		public <T extends Keyed> @NotNull List<TagSource<T>> get(TagOrigin origin, TagType<T> key) {
			List<TagSource<T>> sources = new ArrayList<>();
			for (TagSource<?> source : map.get(key)) {
				if (source.getOrigin().matches(origin))
					//noinspection unchecked
					sources.add((TagSource<T>) source);
			}
			return sources;
		}

		public <T extends Keyed> boolean containsKey(TagType<T> type) {
			return map.containsKey(type);
		}
	}
}

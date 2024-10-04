package org.skriptlang.skript.bukkit.tags;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The type of a tag. Represents a category or context that the tags apply in.
 * For example, {@link #ITEMS} tags apply to {@link Material}s, like {@link #BLOCKS}, but in item form rather than as
 * placed blocks.
 * <br>
 * This class also contains a static registry of all tag types.
 *
 * @param pattern The pattern to use when construction the selection Skript pattern.
 * @param type The class this tag type applies to.
 * @param <T> see type.
 */
public record TagType<T extends Keyed>(String pattern, Class<T> type) {

	private static final List<TagType<?>> REGISTERED_TAG_TYPES = new ArrayList<>();

	public static final TagType<Material> ITEMS = new TagType<>("item", Material.class);
	public static final TagType<Material> BLOCKS = new TagType<>("block", Material.class);
	public static final TagType<EntityType> ENTITIES = new TagType<>("entity [type]", EntityType.class);

	static {
		TagType.addType(ITEMS, BLOCKS, ENTITIES);
	}

	/**
	 * Adds types to the registered tag types.
	 * @param type The types to add.
	 */
	private static void addType(TagType<?>... type) {
		REGISTERED_TAG_TYPES.addAll(List.of(type));
	}

	/**
	 * @return An unmodifiable list of all the registered types.
	 */
	@Contract(pure = true)
	public static @NotNull @UnmodifiableView List<TagType<?>> getTypes() {
		return Collections.unmodifiableList(REGISTERED_TAG_TYPES);
	}

	/**
	 * Gets tag types by index. If a negative value is used, gets all the tag types.
	 * @param i The index of the type to get.
	 * @return The type at that index, or all tags if index < 0.
	 */
	public static TagType<?> @NotNull [] getType(int i) {
		if (i < 0)
			return REGISTERED_TAG_TYPES.toArray(new TagType<?>[0]);
		return new TagType[]{REGISTERED_TAG_TYPES.get(i)};
	}

	/**
	 * @return Returns an optional choice pattern for use in Skript patterns. Contains parse marks.
	 *			Subtract 1 from the parse mark and pass the value to {@link #getType(int)} to get the
	 *			selected tag type in
	 *			{@link ch.njol.skript.lang.SyntaxElement#init(Expression[], int, Kleenean, SkriptParser.ParseResult)}.
	 */
	public static @NotNull String getFullPattern() {
		StringBuilder fullPattern = new StringBuilder("[");
		int numRegistries = REGISTERED_TAG_TYPES.size();
		for (int i = 0; i < numRegistries; i++) {
			fullPattern.append(i + 1).append(":").append(REGISTERED_TAG_TYPES.get(i).pattern());
			if (i + 1 != numRegistries)
				fullPattern.append("|");
		}
		fullPattern.append("]");
		return fullPattern.toString();
	}

}

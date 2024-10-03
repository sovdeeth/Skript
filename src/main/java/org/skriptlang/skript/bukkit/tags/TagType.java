package org.skriptlang.skript.bukkit.tags;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record TagType<T extends Keyed>(String pattern, Class<T> type) {

	private static final List<TagType<?>> REGISTERED_TAG_TYPES = new ArrayList<>();

	public static final TagType<Material> ITEMS = new TagType<>("item", Material.class);
	public static final TagType<Material> BLOCKS = new TagType<>("block", Material.class);
	public static final TagType<EntityType> ENTITIES = new TagType<>("entity [type]", EntityType.class);

	static {
		TagType.addType(ITEMS, BLOCKS, ENTITIES);
	}


	public static void addType(TagType<?>... type) {
		REGISTERED_TAG_TYPES.addAll(List.of(type));
	}

	public static <T extends Keyed> void addType(String pattern, Class<T> type) {
		REGISTERED_TAG_TYPES.add(new TagType<>(pattern, type));
	}

	@Contract(pure = true)
	public static @NotNull @UnmodifiableView List<TagType<?>> getTypes() {
		return Collections.unmodifiableList(REGISTERED_TAG_TYPES);
	}

	public static TagType<?> @NotNull [] getType(int i) {
		if (i < 0)
			return REGISTERED_TAG_TYPES.toArray(new TagType<?>[0]);
		return new TagType[]{REGISTERED_TAG_TYPES.get(i)};
	}

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

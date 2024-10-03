package org.skriptlang.skript.bukkit.tags.sources;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.tags.TagType;

public class BukkitTagSource<T extends Keyed> extends TagSource<T> {

	private final String registry;

	public BukkitTagSource(String registry, TagType<T> type) {
		super(type);
		this.registry = registry;
	}

	@Override
	public @NotNull Iterable<Tag<T>> getAllTags() {
		return Bukkit.getTags(registry, getTypes()[0].type());
	}

	@Override
	public @Nullable Tag<T> getTag(NamespacedKey key) {
		return Bukkit.getTag(registry, key, getTypes()[0].type());
	}
}

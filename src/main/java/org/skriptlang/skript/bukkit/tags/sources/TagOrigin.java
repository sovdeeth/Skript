package org.skriptlang.skript.bukkit.tags.sources;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum TagOrigin {
	BUKKIT,
	PAPER,
	SKRIPT,
	ANY;

	@Contract(pure = true)
	public static @NotNull String getFullPattern() {
		return "[:minecraft|:datapack|:paper|:custom]";
	}

	@Contract(value = "_ -> new", pure = true)
	public static TagOrigin fromParseTags(@NotNull List<String> tags) {
		TagOrigin origin = TagOrigin.ANY;
		if (tags.contains("minecraft") || tags.contains("datapack")) {
			origin = TagOrigin.BUKKIT;
		} else if (tags.contains("paper")) {
			origin = TagOrigin.PAPER;
		} else if (tags.contains("custom")) {
			origin = TagOrigin.SKRIPT;
		}
		return origin;
	}

	public boolean matches(TagOrigin other) {
		return this == other || this == ANY || other == ANY;
	}

	@Contract(pure = true)
	public @NotNull String toString(boolean datapackOnly) {
		return switch (this) {
			case BUKKIT -> datapackOnly ? "datapack " : "minecraft ";
			case PAPER -> "paper ";
			case SKRIPT -> "custom ";
			case ANY -> "";
		};
	}
}

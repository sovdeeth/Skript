package org.skriptlang.skript.bukkit.tags;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.skriptlang.skript.bukkit.tags.TagModule.REGISTRIES;

public class ExprTagsOfType extends SimpleExpression<Tag> {

	static {
		Skript.registerExpression(ExprTagsOfType.class, Tag.class, ExpressionType.SIMPLE,
				"[all [[of] the]] [minecraft] " + TagModule.REGISTRIES_PATTERN + " tags");
	}
	int type;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		type = parseResult.mark - 1;
		return true;
	}

	@Override
	protected Tag<?> @Nullable [] get(Event event) {

		Set<Tag<?>> tags = new TreeSet<>(Comparator.comparing(Keyed::key));
		List<TagModule.RegistryInfo> registries;
		if (type == -1) {
			registries = REGISTRIES;
		} else {
			registries = Collections.singletonList(REGISTRIES.get(type));
		}
		for (TagModule.RegistryInfo registry : registries) {
			tags.addAll((java.util.Collection<? extends Tag<?>>) Bukkit.getTags(registry.registry(), registry.type()));
		}

		// try paper keys if they exist
		if (!TagModule.PAPER_TAGS_EXIST)
			return tags.toArray(new Tag[0]);

		// fallback to paper material tags
		if (type == -1 || REGISTRIES.get(type).type() == Material.class) {
			tags.addAll(TagModule.PAPER_MATERIAL_TAGS);
		}

		// fallback to paper entity tags
		if (type == -1 || REGISTRIES.get(type).type() == EntityType.class) {
			tags.addAll(TagModule.PAPER_ENTITY_TAGS);
		}
		return tags.toArray(new Tag[0]);
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<Tag> getReturnType() {
		return Tag.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		String registry = type == -1 ? "" : REGISTRIES.get(type).pattern() + " ";
		return "all of the minecraft " + registry + "tags";
	}
}

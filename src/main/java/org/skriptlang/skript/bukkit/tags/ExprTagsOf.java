package org.skriptlang.skript.bukkit.tags;

import ch.njol.skript.Skript;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.tags.TagModule.RegistryInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.skriptlang.skript.bukkit.tags.TagModule.REGISTRIES;

public class ExprTagsOf extends PropertyExpression<Object, Tag> {

	static {
		Skript.registerExpression(ExprTagsOf.class, Tag.class, ExpressionType.PROPERTY,
				"[all [[of] the]] [minecraft] " + TagModule.REGISTRIES_PATTERN + " tags of %itemtype/entity/entitydata%",
				"%itemtype/entity/entitydata%'[s] [minecraft] tags");
	}

	int type;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		this.setExpr(expressions[0]);
		type = parseResult.mark - 1;
		return true;
	}

	@Override
	protected Tag<?> @Nullable [] get(Event event, Object[] source) {
		if (source.length == 0)
			return null;
		Keyed value = TagModule.getKeyed(source[0]);
		if (value == null)
			return null;

		Set<Tag<?>> tags = new TreeSet<>(Comparator.comparing(Keyed::key));
		List<RegistryInfo> registries;
		if (type == -1) {
			registries = REGISTRIES;
		} else {
			registries = Collections.singletonList(REGISTRIES.get(type));
		}
		for (RegistryInfo registry : registries) {
			if (value.getClass() == registry.type())
				tags.addAll(getTags(value, registry.registry()));
		}

		// try paper keys if they exist
		if (!TagModule.PAPER_TAGS_EXIST)
			return tags.toArray(new Tag[0]);

		// fallback to paper material tags
		if (value.getClass() == Material.class && (type == -1 || REGISTRIES.get(type).type() == Material.class)) {
			for (Tag<Material> paperMaterialTag : TagModule.PAPER_MATERIAL_TAGS) {
				if (paperMaterialTag.isTagged((Material) value))
					tags.add(paperMaterialTag);
			}
		}

		// fallback to paper entity tags
		if (value.getClass() == EntityType.class && (type == -1 || REGISTRIES.get(type).type() == EntityType.class)) {
			for (Tag<EntityType> paperEntityTag : TagModule.PAPER_ENTITY_TAGS) {
				if (paperEntityTag.isTagged((EntityType) value))
					tags.add(paperEntityTag);
			}
		}

		return tags.toArray(new Tag[0]);
	}

	public static <T extends Keyed> List<Tag<T>> getTags(Keyed value, String registry) {
		List<Tag<T>> tags = new ArrayList<>();

		// Capture the class type of the value
		//noinspection unchecked
		Class<T> clazz = (Class<T>) value.getClass();

		// Fetch and process tags
		for (Tag<T> tag : Bukkit.getTags(registry, clazz)) {
			//noinspection unchecked
			if (tag.isTagged((T) value)) {
				tags.add(tag);
			}
		}

		return tags;
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
		//noinspection DataFlowIssue
		return "minecraft " + registry + "tags of " + getExpr().toString(event, debug);
	}
}

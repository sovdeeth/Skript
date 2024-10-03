package org.skriptlang.skript.bukkit.tags;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.skriptlang.skript.bukkit.tags.TagModule.RegistryInfo;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static org.skriptlang.skript.bukkit.tags.TagModule.REGISTRIES;

public class ExprTag extends SimpleExpression<Tag> {


	static {
		Skript.registerExpression(ExprTag.class, Tag.class, ExpressionType.COMBINED, "[minecraft] " + TagModule.REGISTRIES_PATTERN + " tag %string%");
	}

	Expression<String> name;
	int type;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		//noinspection unchecked
		name = (Expression<String>) expressions[0];
		type = parseResult.mark - 1;
		return true;
	}

	@Override
	protected Tag<?> @Nullable [] get(Event event) {
		String name = this.name.getSingle(event);
		if (name == null)
			return null;

		NamespacedKey key = NamespacedKey.fromString(name);
		if (key == null)
			return null;


		List<RegistryInfo> registries;
		if (type == -1) {
			registries = REGISTRIES;
		} else if (type < REGISTRIES.size()) {
			registries = Collections.singletonList(REGISTRIES.get(type));
		} else {
			return null;
		}

		Tag<?> tag;
		for (RegistryInfo registry : registries) {
			tag = Bukkit.getTag(registry.registry(), key, registry.type());
			if (tag != null)
				return new Tag[]{tag};
		}

		// try paper keys if they exist
		if (!TagModule.PAPER_TAGS_EXIST)
			return null;


		key = new NamespacedKey("paper", key.value() + "_settag");

		// fallback to paper material tags
		if (type == -1 || REGISTRIES.get(type).type() == Material.class) {
			for (Tag<Material> paperMaterialTag : TagModule.PAPER_MATERIAL_TAGS) {
				if (paperMaterialTag.getKey().equals(key))
					return new Tag[]{paperMaterialTag};
			}
		}

		// fallback to paper entity tags
		if (type == -1 || REGISTRIES.get(type).type() == EntityType.class) {
			for (Tag<EntityType> paperEntityTag : TagModule.PAPER_ENTITY_TAGS) {
				if (paperEntityTag.getKey().equals(key))
					return new Tag[]{paperEntityTag};
			}
		}

		return null;
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Class<? extends Tag> getReturnType() {
		return Tag.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		String registry = type == -1 ? "" : REGISTRIES.get(type).pattern();
		return "minecraft " + registry + "tag " + name.toString(event, debug);
	}

}

package org.skriptlang.skript.bukkit.tags;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExprTag extends SimpleExpression<Tag> {

	private record RegistryInfo(String registry, String pattern, Class<? extends Keyed> type) {}

	// Fluid and GameEvent registries also exist, but are not useful at this time.
	// Any new types added here must be handled in CondIsTagged
	private static final List<RegistryInfo> REGISTRIES = new ArrayList<>(List.of(
			new RegistryInfo(Tag.REGISTRY_ITEMS, "item", Material.class),
			new RegistryInfo(Tag.REGISTRY_BLOCKS, "block", Material.class),
			new RegistryInfo(Tag.REGISTRY_ENTITY_TYPES, "entity [type]", EntityType.class)
		));

	static {
		// build pattern
		StringBuilder registriesPattern = new StringBuilder("[");
		int numRegistries = REGISTRIES.size();
		for (int i = 0; i < numRegistries; i++) {
			registriesPattern.append(i + 1).append(":").append(REGISTRIES.get(i).pattern());
			if (i + 1 != numRegistries)
				registriesPattern.append("|");
		}
		registriesPattern.append("]");

		Skript.registerExpression(ExprTag.class, Tag.class, ExpressionType.COMBINED, "[minecraft] " + registriesPattern + " tag %string%");
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

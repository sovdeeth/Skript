package org.skriptlang.skript.bukkit.tags;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.EntityUtils;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ExprTagContents extends SimpleExpression<Object> {

	static {
		Skript.registerExpression(ExprTagContents.class, Object.class, ExpressionType.PROPERTY,
				"tag (contents|values) of %minecrafttags%",
				"%minecrafttags%'[s] tag (contents|values)");
	}

	private Expression<Tag<?>> tag;

	@Override
	public boolean init(Expression<?> @NotNull [] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		//noinspection unchecked
		tag = (Expression<Tag<?>>) expressions[0];
		return true;
	}

	@Override
	protected Object @Nullable [] get(Event event) {
		Tag<?> tag = this.tag.getSingle(event);
		if (tag == null)
			return null;
		return tag.getValues().stream()
			.map(value -> {
				if (value instanceof Material material) {
					return new ItemType(material);
				} else if (value instanceof EntityType entityType) {
					return EntityUtils.toSkriptEntityData(entityType);
				}
				return null;
			})
			.filter(Objects::nonNull)
			.toArray();
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<?> getReturnType() {
		return Object.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "tag contents of " + tag.toString(event, debug);
	}

}

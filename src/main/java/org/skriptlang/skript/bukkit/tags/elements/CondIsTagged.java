package org.skriptlang.skript.bukkit.tags.elements;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import org.bukkit.Keyed;
import org.bukkit.Tag;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.tags.TagModule;

public class CondIsTagged extends Condition {

	static {
		PropertyCondition.register(CondIsTagged.class, PropertyCondition.PropertyType.BE,
				"tagged (as|with) %minecrafttags%",
				"itemtypes/entities/entitydatas");
	}

	private Expression<Tag<Keyed>> tags;
	private Expression<?> elements;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		this.elements = expressions[0];
		//noinspection unchecked
		this.tags = (Expression<Tag<Keyed>>) expressions[1];
		setNegated(matchedPattern == 1);
		return true;
	}

	@Override
	public boolean check(Event event) {
		Tag<Keyed>[] tags = this.tags.getArray(event);
		if (tags.length == 0)
			return isNegated();
		boolean and = this.tags.getAnd();
 		return elements.check(event, element -> {
			boolean isAny = (element instanceof ItemType itemType && !itemType.isAll());
			Keyed[] values = TagModule.getKeyed(element);
			 if (values == null)
				 return false;

			for (Tag<Keyed> tag : tags) {
				 if (isTagged(tag, values, !isAny)) {
					 if (!and)
						 return true;
				 } else if (and) {
					 return false;
				 }
			 }
			return and;
		}, isNegated());
	}

	private boolean isTagged(Tag<Keyed> tag, Keyed @NotNull [] values, boolean allTagged) {
		for (Keyed value : values) {
			if (tag.isTagged(value)) {
				if (!allTagged)
					return true;
			} else if (allTagged) {
				return false;
			}
		}
		return allTagged;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		String plural = elements.isSingle() ? "is" : "are";
		String negated = isNegated() ? " not" : "";
		return elements.toString(event, debug) + " " + plural + negated + " tagged as " + tags.toString(event, debug);
	}

}

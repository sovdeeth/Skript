package org.skriptlang.skript.bukkit.tags;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.EntityUtils;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.Kleenean;
import org.bukkit.Keyed;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

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
			 Keyed value = null;
			 if (element instanceof Entity entity) {
				 value = entity.getType();
			 } if (element instanceof EntityData<?> data) {
				value = EntityUtils.toBukkitEntityType(data);
			} else if (element instanceof ItemType itemType) {
				 value = itemType.getMaterial();
			 } else if (element instanceof ItemStack itemStack) {
				 value = itemStack.getType();
			 } else if (element instanceof Slot slot) {
				 ItemStack stack = slot.getItem();
				 if (stack == null)
					 return false;
				 value = stack.getType();
			 } else if (element instanceof Block block) {
				 value = block.getType();
			 } else if (element instanceof BlockData data) {
				 value = data.getMaterial();
			 }

			 if (value == null)
				 return false;

			for (Tag<Keyed> tag : tags) {
				 if (tag.isTagged(value)) {
					 if (!and)
						 return true;
				 } else if (and) {
					 return false;
				 }
			 }
			return and;
		}, isNegated());
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		String plural = elements.isSingle() ? "is" : "are";
		String negated = isNegated() ? " not" : "";
		return elements.toString(event, debug) + " " + plural + negated + " tagged as " + tags.toString(event, debug);
	}

}
